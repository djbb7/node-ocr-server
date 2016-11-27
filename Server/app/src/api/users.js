import { Router } from 'express';
import crypto from 'crypto';
import User from './schema';

export default ({ config }) => {

	const secret = 'supersecret';

	function randomToken () {
		return crypto.randomBytes(24).toString('hex');
	}

	let api = Router();

	api.get( '/setup', function( req, res ) {

		// create a sample user
		var peter = new User({ 
			username: 'peterpan', 
			password: 'dreamisover'
		});

		// save the sample user
		peter.save(function(err) {
			if (err) throw err;

			console.log('User saved successfully');
		});

		var harry = new User({
			username: 'harry',
			password: 'potter'
		});

		harry.save(function(err) {
			if (err) throw err;

			res.json({ success: true });
		});
	});

	api.post( '/login', ( req, res, next )  => {
		if (!req.body.username || !req.body.password){
			next({code: 400, message: 'Username of password missing.'});
		}
		next();
	}).use( ( req, res, next ) => {
		User.findOne({
				username: req.body.username
			}, function(err, user) {

				if (err) {
					return next({code: 400, message: err});
				}

				if (!user || user.password !== req.body.password) {
					return next({ code: 404, message: 'Authentication failed.' });
				}

				// create a token
				var token = randomToken();

				// TODO: save token to DB

				// return the information including token as JSON
				res.json({
					success: true,
					message: 'Enjoy your token!',
					token: token
			});
		});
 }).use((err, req, res, next) => res.status(err.code).json(err));

	api.post( '/logout', function( req, res ) {

			// TODO: check token is in DB

			// TODO: delete token from DB
	});

	return api;
}
