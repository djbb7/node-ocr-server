import { Router } from 'express';
import { User, Transaction, Image, File} from './schema';
import { check_user } from './users';
import { getTransactionJSON } from './ocr';

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

			let response = {
				transactions: []
			};

			transactions.forEach(transaction => {
				let ta = getTransactionJSON(transaction);
				ta.id = transaction._id;
				ta.href = '/ocr/transaction/' + transaction._id;
				
				response.transactions.push(ta);
			});

			res.json(response);
		});
	});

	return history;
}
