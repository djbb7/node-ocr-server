# Mobile Cloud Computing (Fall 2016) - Project 2
## Group 15 - Open Oranges

This piece of software allows user can extract text from a source image by an application
on Android phone as well as offload computation to the servers.It has 3 components:
 - Backend: manages user authentication, and extract data from images Android application sent.
 - Storage: cluster of database store output text and other data such as thumbnail, meta data and source images (short time).
 - Frontend: Android Application can extract text from an image through OCR, as well as benchmarking performance between Local and remote mode.

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


# Implementation Details

## Backend Server
- Made with Node.js
- RESTful, speaks JSON.
- Generates a security token which the client must send in each sucessive request.
When the user logs out the token is invalidated.
- Process images Android app sent to backend to extract the text.
- The code is mostly written by Daniel Bruzual, and Jasse Lahdenperä.

## MongoDB Cluster
- Use Kurbernetes creat cluster, replica management and service for MongoReplica set.
- Create a lightweight service on top of cluster to gurantee fault tolerance. 
- Intergate backend and cluster.
The code and deployment script is based on https://github.com/cvallance/mongo-k8s-sidecar
- This part is implemented by Viet Do.

## Android Client

