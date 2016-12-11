# Mobile Cloud Computing (Fall 2016) - Project 2
## Group 15 - Open Oranges


# Installation

The project contains 3 folders:
  - **Android**: Android studio project of the android client
  - **server**: Backend code that handles authentication 
  - **Config**: Store configuration data to deployment


Additionally, there is a script named **deploy.sh** which allows to deploy the backend
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
Google Cloud is created with 3 nodes. Kubernetes use the Docker file to deploy 3 instances of backend server for load balancing and fault tolerance. 

After these instances of backend application are created, the external IP address for the backend application will be updated to duckDNS. Android application will use this domain to access the service. 

#### MongoDB Cluster

This continued creating a cluster of 3 MongoDB replicas, assign permanent hard drives, services and replica management for controlling pods. In addition, to ensure database cluster
is fault tolerant, a lightweight service is built on top of replica. This guarantees that these replicas are on different nodes of the cluster.

#### Android APK  

The android apk will be generated inside the Android folder, with the name: mcc_fall_2016_g15.apk

# Using the Android App

**Login View**
As a default the login page is reached.
Two credentials are set up.

Credential 1:
```
username: peterpan
password: dreamisover
```

Credential 2:
```
username: harry
password: potter
```

**History view**
After logging in, a history of the former proceeded images of the user will be shown.
(Only the remotely proceeded OCRs will be retrieved.)
By clicking on one item in the list, a detailed view of this transaction is reached (Show view).
Each entry is displayed by a thumbnail of the proceeded image and a preview of the resulting text.
At the top menu bar the user can refresh the history or logout.
On the right bottom side, there is a add-button, which allows the user to take a new picture and proceed the OCR.

**Takepicture view**
Upon pressing the add-button, the camera will open. There is a button to take a picture or select an image from the gallery.
(It is possible to choose several images from the gallery.)
In the top menu bar the user can go back to the history or change the mode, how the OCR on the image should be proceeded.
The default mode is remote. Other options are local or benchmark.

**ProcessOCR view**
After a picture is taken either by the camera or the gallery, it is displayed with a bottom menu bar.
One option is to retake the picture (Takepicture view) and the second option is to proceed the OCR upon your selected mode.
The ongoing process then will be displayed by a progress bar.

**Show view**
After a succesfull OCR processing either locally or remotely, the extracted text and the timestamp will be displayed.
On the top menu bar the user can got back to the history view.
On the bottom menu bar three options are available:
- Retake: Takes the user back to the takepicture view to retake the picture, if the result is unsatisfying.
- Camera symbol: The proceeded image is shown. On repeated clicking the text is shown again.
- Save text: The resulted text is saved on the device in the folder "OpenTxtFiles".

**Benchmark view**
If the mode benchmark is selected, then an overview of the benchmarking is reached.
Local and remote OCR are compared.
On the top menu bar the user can reach its history again. 
