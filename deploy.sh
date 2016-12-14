#!/bin/bash

#location of things inside project folder
SERVER_PATH=Server
ANDROID_PATH=Android/OpenOCRanges
CONFIG_PATH=config
WD=`pwd`

PROJECT_ID=mcc-2016-g15-p2
SERVICE_FILE="${CONFIG_PATH}/mcc-2016-g15-p2-ce172c4f2841.json"
export GOOGLE_APPLICATION_CREDENTIALS="${WD}/${SERVICE_FILE}"
DOCKER_IMAGE="gcr.io/mcc-2016-g15-p2/ocr-server"
CLUSTER_NAME="ocr-server"
CLUSTER_MAKEFILE="${SERVER_PATH}/k8s-sidecar-edit/"
ZONE="europe-west1-d"

#color printing
RED='\033[0;31m'
ORANGE='\033[0;33m'
YELLOW='\033[0;'
GREEN='\033[0;32m'
NC='\033[0m'

#Print team name
echo -e "${ORANGE}"
cat ${CONFIG_PATH}/asciiart
echo -e "${NC}"

######################
# install dependencies

#update repositories
sudo apt-get update

#install docker
if hash docker 2>/dev/null; then
    echo -e "\n\n${ORANGE}Docker already installed...${NC}"
else
    echo -e "\n\n${ORANGE}Installing docker...${NC}"
    # curl -fsSL https://get.docker.com/ | sh
    sudo apt-get install docker.io
    echo -e "\n\n${ORANGE}Docker installed...${NC}"
    
    sudo service docker start
    WHO=`whoami`
    sudo usermod -aG docker $WHO

    echo -e "\n\n${ORANGE}Docker installed...${NC}"
    echo -e "${ORANGE}Please logout and log back in, and rerun script...${NC}"
    

fi

#install gcloud
if hash gcloud 2>/dev/null; then
    echo -e "\n\n${ORANGE}gcloud already installed...${NC}"
else
    echo -e "\n\n${ORANGE}Installing gcloud...${NC}"
    export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)"
    echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
    sudo apt-get update && sudo apt-get install google-cloud-sdk
    echo -e "\n\n${ORANGE}gcloud installed...${NC}"
fi

#install kubernetes
if hash gcloud 2>/dev/null; then
    echo -e "\n\n${ORANGE}kubectl already installed...${NC}"
else
    echo -e "\n\n${ORANGE}Installing kubectl...${NC}"
    #gcloud components install kubectl
    curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
    chmod +x ./kubectl
    sudo mv ./kubectl /usr/local/bin/kubectl
    echo -e "\n\n${ORANGE}kubectl installed...${NC}"
fi

######################
# configure

#set gcloud account
gcloud config set account mcc.fall.2016.g15@gmail.com

#authenticate gcloud
gcloud auth activate-service-account proj-owner@mcc-2016-g15-p2.iam.gserviceaccount.com --key-file $SERVICE_FILE

#authenticate kubernetes
#gcloud auth application-default login proj-owner@mcc-2016-g15-p2.iam.gserviceaccount.com --key-file $SERVICE_FILE

# set gcloud project id
gcloud config set project $PROJECT_ID

# set gcloud default zone
gcloud config set compute/zone europe-west1-d	


######################
# build and deploy

#build docker image
echo -e "\n\n${ORANGE}Building docker image...${NC}"
docker build -t $DOCKER_IMAGE $SERVER_PATH

#send image to registry
echo -e "\n\n${ORANGE}Adding image to gcloud registry...${NC}"
gcloud docker -- push $DOCKER_IMAGE

#create gcloud cluster
echo -e "\n\n${ORANGE}Creating gcloud cluster...${NC}"
gcloud container clusters create $CLUSTER_NAME

#deploy using kubernetes
echo -e "\n\n${ORANGE}Creating kubernetes deployment...${NC}"
kubectl run ocr-server --image=$DOCKER_IMAGE --replicas=3 --port=8080

echo -e "\n\n${ORANGE}Exposing kubernetes deployment...${NC}"
kubectl expose deployment ocr-server --port=80 --target-port=8080 --type="LoadBalancer"

#get IP and set duckdns
echo -e "\n\n${ORANGE}Waiting for IP address to be assigned...${NC}"
while true; do
    IP=$(kubectl get service ocr-server | tail -1 | awk '{print $3}')
    if [ "$IP" != "<pending>" ]; then
        break
    fi

    sleep 2
done;

echo -e "\n\n${ORANGE}Setting IP to dynamic DNS...${NC}"
curl "https://www.duckdns.org/update?domains=mcc2016g15p2&token=6ad8a0b3-7a22-49da-91b0-05d5745565d5&ip=${IP}&verbose=true"

#create mongodb cluster

# get cluster credential
echo -e "\n\n${ORANGE}Getting cluster credential...${NC}"
gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE 

# create replica set
echo -e "\n\n${ORANGE}Creating Mongo Cluster...${NC}"
cd $CLUSTER_MAKEFILE
a=1
while [ $a -le 3 ]
do
  make add-replica
  a=$(( a+1 ))
done
make create-null-service
cd ../../

#build android apk
echo -e "\n\n${ORANGE}Building Android apk...${NC}"

#build apk
echo -e "${ORANGE}[Using gradle to get dependencies, it may take a while]${NC}"
read -n 1 -s -p "Press any key to start build..."

cd $ANDROID_PATH
chmod +x gradlew
{
    ./gradlew assemble &&
    #move apk to results directory
    mv app/build/outputs/apk/app-debug.apk ../mcc_fall_2016_g15.apk &&
    echo -e "${GREEN}apk generated: ${WD}/mcc_fall_2016_g15.apk${NC}"
} || {
    echo -e "${RED}could not build apk${NC}"
}
cd $WD