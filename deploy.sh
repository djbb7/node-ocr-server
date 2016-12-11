#!/bin/bash

#location of things inside project folder
SERVER_PATH=Server
ANDROID_PATH=Android
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

#install docker

#install gcloud
#apt-get update
#apt-get install python
#wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-137.0.1-linux-x86_64.tar.gz
#tar xvzf google-cloud-sdk-137.0.1-linux-x86_64.tar.gz
#cd google-cloud-sdk
#./install.sh
#cd ..
#source .bashrc

#install kubernetes
# gcloud components install kubectl

######################
# configure

#authenticate gcloud
gcloud auth activate-service-account proj-owner@mcc-2016-g15-p2.iam.gserviceaccount.com --key-file $SERVICE_FILE

#authenticate kubernetes
gcloud auth application-default login proj-owner@mcc-2016-g15-p2.iam.gserviceaccount.com --key-file $SERVICE_FILE

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
kubectl expose deployment ocr-server --port=443 --target-port=8080 --type="LoadBalancer"

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
curl "https://www.duckdns.org/update?domains=openocranges&token=6ad8a0b3-7a22-49da-91b0-05d5745565d5&ip=${IP}&verbose=true"

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

#
