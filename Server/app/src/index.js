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

const options = {
	key: fs.readFileSync(__dirname + '/../ssl/key.pem'),
	cert: fs.readFileSync(__dirname + '/../ssl/cert.pem')
};

let app = express();

app.server = https.createServer(options, app);

app.use(bodyParser.json({
	limit : config.bodyLimit
}));


mongoose.Promise = global.Promise;
mongoose.connect(config.database); // connect to database

app.use('/users', users( { config } ) );

app.use('/ocr', ocr( { config } ) );

app.use('/history', history( { config } ) );

// expose some API metadata at the root
app.get('/', (req, res) => {
	res.json({ version });
});


app.use(express.static(__dirname + '/public'));

app.server.listen(process.env.PORT || config.port);

console.log(`Started on port ${app.server.address().port}`);

export default app;
