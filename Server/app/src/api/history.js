import { Router } from 'express';
import { User, Transaction, Image, File} from './schema';
import { check_user } from './users';

export default ({ config }) => {
	let history = Router();

	history.get('/', check_user, (req, res) => {
		var skip = 0;
		var limit = 10;

		if(req.query.skip)
			skip = req.query.skip;

		if(req.query.limit)
			limit = req.query.limit;

		Transaction.find({
			_user: req.user._id
		}).
		sort({ createdAt: -1}).
		skip(skip).
		populate('files').
		exec((err, transaction) => {
			return;
		});
	});

	return history;
}
