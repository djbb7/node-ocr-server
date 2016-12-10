import { Router } from 'express';
import crypto from 'crypto';
import { User, Session } from './schema';
import passport from 'passport';

export default ({ config }) => {

	function randomToken () {
		return crypto.randomBytes(24).toString('hex');
	}

	let users = Router();

	users.get( '/setup', function( req, res ) {

		// create a sample user
		var peter = new User({ 
			username: 'peterpan', 
			password: 'dreamisover'
		});

		// save the sample user
		peter.save(function(err) {
			if (err) throw err;
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

	users.post('/login', ( req, res, next ) => {
		if(!req.body.username || !req.body.password) {
			next({ code: 400, message: 'Username of password missing.' });
		}
		next();
	}, (req, res, next) => {
		User.findOne({username: req.body.username}, (err, user) => {
			if(err)
				return next({ code: 400, message: err });

			if(!user || user.password !== req.body.password)
				return next({ code: 404, message: 'Authentication failed.' });

			// Create a token
			var token = randomToken();

			// Create a session
			var session = new Session({
				_user: user,
				token: token
			});
			session.save();

			// Return the information including token as JSON
			res.json({
				token: token
			});
		});
 	}, (err, req, res, next) => { res.status(err.code).json(err); });

	users.post('/logout', check_user, function( req, res ) {

		Session.remove({token: req.get('Authorization')}).exec((err) => {
			if (err) {
				res.status(500).send("Could not delete session.");
			}

			res.sendStatus(200);
		});

	});

	return users;
}

export function check_user(req, res, next) {
	if(!req.get('Authorization'))
	{
		res.status(401).send('Not logged in');
		return;
	}

	Session.findOne({token: req.get('Authorization')}).populate('_user').exec((err, session) => {
		if(err || session === null || session._user === null)
		{
			res.status(401).send('Session expired');
			return;
		}

		req.session = session;
		req.user = session._user;
		next();
	});
}
