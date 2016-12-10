import mongoose from 'mongoose';
import config from '../config.json';

let Schema = mongoose.Schema;

export const User = mongoose.model('User', new Schema({
	username: String,
	password: String,
	transactions: [{ type: Schema.Types.ObjectId, ref: 'Transaction' }]
}));

export const Session = mongoose.model('Session', new Schema({
	_user: { type: Schema.Types.ObjectId, ref: 'User' },
	token: String,
	started: { type: Date, expires: 3600, default: Date.now }
}));

export const Transaction = mongoose.model('Transaction', new Schema({
	_user: { type: Schema.Types.ObjectId, ref: 'User' },
	files: [{ type: Schema.Types.ObjectId, ref: 'File' }],
	createdAt: { type: Date, default: Date.now },
	finishedAt: { type: Date, default: null }
}));

export const File = mongoose.model('File', new Schema({
	_transaction: { type: Schema.Types.ObjectId, ref: 'Transaction'},
	fileName: String,
	extractedText: String,
	error: String,
	thumbnail: Buffer,
	processingStarted: Date,
	processingFinished: Date,
	image: { type: Schema.Types.ObjectId, ref: 'Image' }
}));

// Images with 30min TTL
export const Image = mongoose.model('Image', new Schema({
	_user: { type: Schema.Types.ObjectId, ref: 'User' }, // Only used for access control
	data: Buffer,
	createdAt: { type: Date, expires: parseInt(process.env.SOURCE_IMAGE_LIFETIME) || config.SOURCE_IMAGE_LIFETIME, default: Date.now }
}));

export function setupUsers(){
	User.find({}, function(err, users){
		if (users.length == 0) {
			// create a sample user
			var peter = new User({ 
				username: 'peterpan', 
				password: 'dreamisover'
			});

			// save the sample user
			peter.save(function(err) {
				if (err) throw err;

				console.log('Created user \'peterpan\' password \'dreamisover\'');
			});

			var harry = new User({
				username: 'harry',
				password: 'potter'
			});

			harry.save(function(err) {
				if (err) throw err;
				
				console.log('Created user \'harry\' password \'potter\'');
			});
		}
	});
};