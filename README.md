# Mobile Cloud Computing (Fall 2016) - Project 2
## Group 15 - Open Oranges


# Installation

The project contains 3 folders:
  - **Android**: Android studio project of the android client
  - **server**: Backend code that handles authentication and 
  - **Config**: Store configuration data to deployment


Additionally there is script named **deploy.sh** which allows deploying the backend
server and MongoDB replica set as a cluster on Google Cloud by Kubernetes and to build
the android APK.

## Requirements
The code deployment script has been tested on Ubuntu Server 16.04 64-bits running
on VirtualBox. It will automatically deploy backend server's docker file and MongoDB cluster to GoogleCloud.

## Instructions

To install the both components execute the following commands:

```
./deploy.sh
```
#### The backend server
This will download the docker image of Nodejs and pack all dependencies, libraries, and code to a container and put to Gcloud registry. Then, a container cluster on
Google Cloud is created with 3 nodes. Kubernetes use the the Docker file to deploy 3 instances of backend server for load balancing and fault tolerance. 

After these instances of backend application are created, the external IP address for the backend application will be updated to duckDNS. Android application will use this domain to access the service. 

#### MongoDB Cluster

This continued creating a cluster of 3 MongoDB replica, assign permanent hard drives, services and replica management for controlling pods. In addition, to ensure database cluster
is fault tolerant, a lightweight service is built on top o replica. This guarantees these replicas are on different nodes of the cluster.

#### Android APK  

