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


#delete mongo cluster

# get cluster credential
echo -e "\n\n${ORANGE}Getting cluster credential...${NC}"
gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE 

# Delete replica set
echo -e "\n\n${ORANGE}Deleting Mongo Cluster...${NC}"
cd $CLUSTER_MAKEFILE
a=1
while [ $a -le 3 ]
do
  make delete-replica
  a=$(( a+1 ))
done
make delete-null-service
#cd ../../

echo -e "\n\n${ORANGE}Delete cluster...${NC}"
gcloud container clusters delete $CLUSTER_NAME --zone=$ZONE

