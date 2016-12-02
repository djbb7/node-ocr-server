import https from 'https';
import express from 'express';
import cors from 'cors';
import bodyParser from 'body-parser';
import mongoose from 'mongoose';
import config from './config.json';
import fs from 'fs';
import users from './api/users';
import ocr from './api/ocr';
import history from './api/history';
import { version } from '../package.json';
import multer from 'multer';

// Setup self-signed certificate
const options = {
	key: fs.readFileSync(__dirname + '/../ssl/key.pem'),
	cert: fs.readFileSync(__dirname + '/../ssl/cert.pem')
};

// setup middleware for handling multiform/form-data
let memoryStorage = multer.memoryStorage();
let upload = multer({
	storage : memoryStorage
});

// setup mongodb connection
mongoose.Promise = global.Promise;
mongoose.set('debug', true);
mongoose.connect(config.database);


let app = express();

// setup middleware for accessing JSON request
app.use(bodyParser.json({
	limit : config.bodyLimit
}));

// API routes
app.use('/users', users( { config } ) );
app.use('/ocr', ocr( { config }, upload ) );
app.use('/history', history( { config } ) );

// route for serving static files
app.use(express.static(__dirname + '/public'));

// simple endpoint to check server is running
app.get('/', (req, res) => {
	res.json({ version });
});

app.server = https.createServer(options, app);

app.server.listen(process.env.PORT || config.port);

console.log(`Started on port ${app.server.address().port}`);

export default app;
