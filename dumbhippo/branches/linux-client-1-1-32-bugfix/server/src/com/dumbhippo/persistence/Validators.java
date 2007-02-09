package com.dumbhippo.persistence;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class Validators {
	private static final Logger logger = GlobalSetup.getLogger(Validators.class); 
	
	/**
	 * Be sure a stock photo name looks plausible and can be stuck in HTML without escaping.
	 * Expects the url to be in the form we'd put in the database, currently always 
	 * /user_pix1/foo.png or /group_pix1/foo.png
	 * 
	 * @param relativePhotoUrl
	 * @return true if it's OK
	 */
	public static boolean validateStockPhoto(String relativePhotoUrl) {
		int dirLen;
		if (relativePhotoUrl.startsWith("/user_pix1/")) {
			dirLen = "/user_pix1/".length();
		} else if (relativePhotoUrl.startsWith("/group_pix1/")) {
			dirLen = "/group_pix1/".length();
		} else {
			logger.debug("Invalid stock photo name '{}'", relativePhotoUrl);
			return false;
		}
		
		if (!relativePhotoUrl.endsWith(".png")) {
			logger.debug("Invalid stock photo name '{}'", relativePhotoUrl);
			return false;
		}
		
		String basename = relativePhotoUrl.substring(dirLen, relativePhotoUrl.length() - ".png".length());
				
		for (char c : basename.toCharArray()) {
			if (c < 'a' || c > 'z') {
				logger.debug("Invalid stock photo name '{}'", relativePhotoUrl);
				return false;
			}
		}
		
		return true;
	}
}
