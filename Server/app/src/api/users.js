import { Router } from 'express';
import	mongoose	from 'mongoose';
import	jwt from 'jsonwebtoken';

export default ({ config }) => {
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
			res.json({ success: true });
		});
	});

	api.post( '/login', function( req, res ) {
		User.findOne({
			username: req.body.username
		}, function(err, user) {

			if (err) throw err;

			if (!user) {
				res.json({ success: false, message: 'Authentication failed. User not found.' });
			} else if (user) {

				// check if password matches
				if (user.password != req.body.password) {
					res.json({ success: false, message: 'Authentication failed. Wrong password.' });
				} else {
					// if user is found and password is right
					// create a token
					var token = jwt.sign(user, 'secret', {
						expiresIn: 1440 // expires in 24 hours
					});

					// return the information including token as JSON
					res.json({
						success: true,
						message: 'Enjoy your token!',
						token: token
					});
				}
			}
		});
 });

	return api;
}

let Schema = mongoose.Schema;

export const User = mongoose.model('User', new Schema({ 
		username: String, 
		password: String
}));