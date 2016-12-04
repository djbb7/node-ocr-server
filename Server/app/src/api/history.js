import { Router } from 'express';
import { User, Transaction, Image, File} from './schema';
import { check_user } from './users';

export default ({ config }) => {
	let history = Router();

	history.get('/', check_user, (req, res) => {
		var skip = 0;
		var limit = 10;

		if(req.query.skip)
			skip = parseInt(req.query.skip);

		if(req.query.limit)
			limit = parseInt(req.query.limit);

		Transaction.find({
			_user: req.user._id
		}).
		sort({ createdAt: 1}).
		skip(skip).
		limit(limit).
		populate('files').
		exec((err, transactions) => {
			console.log(err);

			var response = {
				transactions: []
			};

			transactions.forEach(transaction => {
				var ta = {
					id: transaction._id,
					href: '/ocr/transaction/' + transaction._id,
					createdAt: transaction.createdAt,
					finishedAt: transaction.finishedAt,
					duration: (transaction.finishedAt - transaction.createdAt) / 1000,
					files: []
				};

				transaction.files.forEach(file => {
					ta.files.push({
						fileName : file.fileName,
						extractedText: file.extractedText,
						error: file.error,
						processingStarted: file.processingStarted,
						processingFinished: file.processingFinished,
						processingTime: (file.processingFinished - file.processingStarted) / 1000,
						thumbnailUrl: '/ocr/thumb/' + file._id,
						imageUrl: '/ocr/image/' + file.image
					});
				});
				response.transactions.push(ta);
			});

			res.json(response);
		});
	});

	return history;
}
