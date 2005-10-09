/**
 * 
 */
package com.dumbhippo.jive;

import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.util.Log;

/**
 * @author hp
 *
 */
public class HippoAuthProvider implements
		org.jivesoftware.messenger.auth.AuthProvider {

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#isPlainSupported()
	 */
	public boolean isPlainSupported() {
		
		Log.debug("isPlainSupported()");
		
		// FIXME turn this off
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#isDigestSupported()
	 */
	public boolean isDigestSupported() {
		
		Log.debug("isDigestSupported()");
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String password)
			throws UnauthorizedException {
	
		Log.debug("authenticate() username = " + username + " password = " + password);
		
		// throw new UnsupportedOperationException("Plain text passwords are not supported");
		// FIXME
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException {
		
		Log.debug("authenticate() username = " + username + " token = " + token + " digest = " + digest);
		
		if (HippoUserProvider.ENABLE_ADMIN_USER) {
			if (username.equals(HippoUserProvider.ADMIN_USERNAME)) {
				// FIXME check a password
				return;
			}
		}
		
		if (!Server.getMessengerGlue().authenticateJabberUser(username, token, digest))
			throw new UnauthorizedException("Not authorized");
	}
}
