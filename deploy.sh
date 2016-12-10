#!/bin/bash

#location of things inside project folder
SERVER_PATH=Server
ANDROID_PATH=Android
CONFIG_PATH=config
WD=`pwd`

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

#authenticate kubernetes

######################
# build and deploy

#build docker image

#send image to registry

#create gcloud cluster

#create mongodb cluster

#build android apk

