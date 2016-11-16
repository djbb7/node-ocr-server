import { Router } from 'express';
import upload from '../index';
import multer from 'multer';
import mongoose from 'mongoose';

export default ({ config }, upload) => {
	let ocr = Router();

	ocr.post('/upload', upload.array('photos[]', 15), function( req, res ) {
		console.log(req);

		req.files.forEach(function(file) {
			var image = new Image({ 
				name: file.originalname,
				data: file.buffer
			});

			image.save();
		});

		res.json({
			'text': 'Lorem ipsum dolor sit amet'
		});
	});

	return ocr;
}

let Schema = mongoose.Schema;

export const Image = mongoose.model('Image', new Schema({ 
	name: String,
	data: Buffer
}));
