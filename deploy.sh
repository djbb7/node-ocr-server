#!/bin/bash

#location of things inside project folder
SERVER_PATH=Server
ANDROID_PATH=Android
CONFIG_PATH=config
WD=`pwd`

PROJECT_ID=mcc-2016-g15-p2
SERVICE_FILE="${CONFIG_PATH}/mcc-2016-g15-p2-ce172c4f2841.json"
DOCKER_IMAGE="gcr.io/mcc-2016-g15-p2/ocr-server"
CLUSTER_NAME="ocr-server"

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

#install java

#install gcloud

#install kubernetes

#install android sdk

######################
# configure

#authenticate gcloud
gcloud auth activate-service-account --key-file $SERVICE_FILE

#authenticate kubernetes

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
curl "https://www.duckdns.org/update?domains=mcc2016g15p2&token=6ad8a0b3-7a22-49da-91b0-05d5745565d5&ip=${IP}&verbose=true"

#create mongodb cluster


#build android apk
echo -e "\n\n${ORANGE}Building Android apk...${NC}"

#