import { Router } from 'express';
import upload from '../index';
import multer from 'multer';
import tesseract from 'node-tesseract';
import fs from 'fs';
import { User, Transaction, Image, File} from './schema';

export default ({ config }, upload) => {
	let ocr = Router();

	ocr.post('/upload', upload.array('photos[]', 15), (req, res) => {
		var transaction = new Transaction({
			_user: null, // TODO: fill with proper value once login is done
			processed: false
		});

		req.files.forEach(function(file) {
			var image = new Image({
				_user: null, // TODO: fill with proper value once login is done
				data: file.buffer
			});
			image.save();

			var file = new File({
				_transaction: transaction,
				fileName: file.originalName,
				extractedText: "",
				thumbnail: file.buffer, // TODO: resize image for thumbnail
				image: image
			});
			file.save();

			transaction.files.push(file);
		});

		// Save transaction and start OCR crunch
		transaction.save().then(() => {
			transaction.populate('files', (err) => {
				transaction.files.forEach(file => {
					process(transaction, file);
				});
			});
		});

		res.json({
			transaction: {
				id: transaction._id,
				href: '/ocr/transaction?id=' + transaction._id
			}			
		});
	});

	ocr.get('/image', (req, res) => {
		var imageId = req.query.id;

		Image.findById(imageId, (err, image) => {
			if(err)
				throw err;

			if(image == null)
			{
				res.status(404).send('Not found');
				return;
			}
			
			res.end(image.data);
		});
	});

	ocr.get('/thumb', (req, res) => {
		var fileId = req.query.id;

		File.findById(fileId, (err, file) => {
			if(err)
				throw err;

			if(file == null)
			{
				res.status(404).send('Not found');
				return;
			}

			res.end(file.thumbnail);
		})
	})

	ocr.get('/transaction', (req, res) => {
		var transactionId = req.query.id;

		Transaction.findById(transactionId).populate('files').exec((err, transaction) => {
			if(err)
				throw err;

			if(transaction == null)
				return next({code: 404});

			var response = {
				files: []
			}

			transaction.files.forEach(file => {
				response.files.push({
					fileName : file.fileName,
					extractedText: file.extractedText,
					thumbnailUrl: '/ocr/thumb?id=' + file._id,
					imageUrl: '/ocr/image?id=' + file.image
				});
			});

			res.json(response);
		});
	})

	return ocr;
}

function process(transaction, file) {
	console.log('Processing file');
	file.populate('image', (err) => {
		if(err)
			throw err;

		if(file.image == null)
		{
			console.log('Image not found for file ' + file._id);
			return;
		}

		console.log('Processing ' + file.image._id);

		// Tesseract reads file from disk so create temporary file
		let fileName = __dirname + '/' + file.image._id;
		fs.writeFile(fileName, file.image.data);

		// Start processing image, will return automatically when done
		tesseract.process(fileName, (err, text) => {
			if(err) {
				fs.unlink(fileName);
				console.log('Processing image ' + file.image._id + ' failed');
			}
			else {
				fs.unlink(fileName);

				console.log('Image ' + file.image._id + ' processed');
				file.extractedText = text;
				file.save();

				// TODO: check state of transaction
			}
		});
	});
}
