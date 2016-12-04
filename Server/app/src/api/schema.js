import mongoose from 'mongoose';

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
	createdAt: { type: Date, expires: 1800, default: Date.now }
}));
