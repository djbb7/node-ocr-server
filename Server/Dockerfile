FROM node:4
ENV USE_SSL 1
ENV MONGODB_URL mongodb://mongo-1,mongo-2,mongo-3:27017/OCR_USERS
EXPOSE 8080
COPY ./app/ .
RUN apt-get -y update && apt-get install -y tesseract-ocr
RUN npm install
CMD ["npm", "start"]
