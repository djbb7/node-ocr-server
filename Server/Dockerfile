FROM node:4
ENV USE_SSL 1
ENV MONGODB_URL mongodb://peterpan:dreamisover@ds119768.mlab.com:19768/mcc-2016-g15-p2
EXPOSE 8080
COPY ./app/ .
RUN apt-get -y update && apt-get install -y tesseract-ocr
RUN npm install
CMD ["npm", "start"]
