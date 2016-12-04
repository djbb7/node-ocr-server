import { Router } from 'express';
import upload from '../index';
import multer from 'multer';
import tesseract from 'node-tesseract';
import fs from 'fs';
import { User, Transaction, Image, File} from './schema';
import Jimp from 'jimp';

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
				extractedText: null,
				error: null,
				thumbnail: null,
				image: image
			});
			file.save();

			// Create thumbnail for the image
			Jimp.read(image.data, (err, image) => {
				image.scaleToFit(256, 256).getBuffer(Jimp.MIME_PNG, (err, buffer) => {
					file.thumbnail = buffer;
					file.save();
				});
			});

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
				href: '/ocr/transaction/' + transaction._id
			}
		});
	});

	ocr.get('/image/:id', (req, res) => {
		var imageId = req.params.id;

		Image.findById(imageId, (err, image) => {
			// CastErrors are ignored since they'll result in 404 in next step
			if(err && err.name !== 'CastError')
				throw err;

			if(image == null)
			{
				res.status(404).send('Not found');
				return;
			}
			
			res.end(image.data);
		});
	});

	ocr.get('/thumb/:id', (req, res) => {
		var fileId = req.params.id;

		File.findById(fileId, (err, file) => {
			// CastErrors are ignored since they'll result in 404 in next step
			if(err && err.name !== 'CastError')
				throw err;

			if(file == null)
			{
				res.status(404).send('Not found');
				return;
			}

			res.end(file.thumbnail);
		})
	})

	ocr.get('/transaction/:id', (req, res) => {
		var transactionId = req.params.id;

		Transaction.findById(transactionId).populate('files').exec((err, transaction) => {
			// CastErrors are ignored since they'll result in 404 in next step
			if(err && err.name !== 'CastError')
				throw err;

			if(transaction == null)
			{
				res.status(404).send('Transaction not found');
				return;
			}

			if(transaction.finishedAt === null)
			{
				res.status(202).send('Not finished');
				return;
			}

			var response = {
				files: [],
				createdAt: transaction.createdAt,
				finishedAt: transaction.finishedAt,
				duration: (transaction.finishedAt - transaction.createdAt) / 1000
			}

			// Craft list of files and their thumbnails and images
			transaction.files.forEach(file => {
				response.files.push({
					fileName : file.fileName,
					extractedText: file.extractedText,
					error: file.error,
					thumbnailUrl: '/ocr/thumb/' + file._id,
					imageUrl: '/ocr/image/' + file.image
				});
			});

			res.json(response);
		});
	})

	return ocr;
}

function process(transaction, file) {
	file.populate('image', (err) => {
		if(err)
			throw err;

		// In practice this should never happen unless someone gives images ridiculously low TTL
		if(file.image === null)
		{
			// Indicate that processing file failed to handle transaction completions properly
			file.error = 'Failed to perform OCR on file, image not found in database.';
			file.save();

			checkCompletion(transaction);

			return;
		}

		// Tesseract reads file from disk so create temporary file
		let fileName = __dirname + '/' + file.image._id;
		fs.writeFile(fileName, file.image.data);

		// Start processing image, will return automatically when done
		tesseract.process(fileName, (err, text) => {
			if(err) {
				fs.unlink(fileName);

				file.error = 'Running tesseract failed: \'' + err + '\'';
				file.save();
			}
			else {
				fs.unlink(fileName);

				file.extractedText = text;
				file.save();
			}

			checkCompletion(transaction);
		});
	});
}

function checkCompletion(transaction)
{
	// Check if transaction finished by completing processing of this file
	if(transaction.finishedAt !== null)
		console.log('Transaction has already been finished');

	// Transaction is completed if every file has either extractedText or error specified
	let done = transaction.files.every(file => {
		return file.extractedText !== null || file.error !== null;
	});

	if(done)
	{
		transaction.finishedAt = Date.now();
		transaction.save();
	}
}
