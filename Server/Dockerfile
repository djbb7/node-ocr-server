FROM node:4
EXPOSE 8080
COPY ./app/ .
RUN apt-get -y update && apt-get install -y tesseract-ocr
RUN npm install
CMD ["npm", "start"]
