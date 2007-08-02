/**
 * 
 */
package com.dumbhippo.jive;

import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.ServerTooBusyException;

/**
 * @author hp
 *
 */
public class HippoAuthProvider implements
		org.jivesoftware.openfire.auth.AuthProvider {

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.auth.AuthProvider#isPlainSupported()
	 */
	public boolean isPlainSupported() {
		
		Log.debug("isPlainSupported()");
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.auth.AuthProvider#isDigestSupported()
	 */
	public boolean isDigestSupported() {
		
		Log.debug("isDigestSupported()");
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.auth.AuthProvider#authenticate(java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String password)
			throws UnauthorizedException {
	
		Log.debug("authenticate() username = " + username + " password = " + password);
		
		throw new UnsupportedOperationException("Plain text passwords are not supported");
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.auth.AuthProvider#authenticate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException {
		
		Log.debug("authenticate() username = " + username + " token = " + token + " digest = " + digest);
		
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		
		if (glue.isServerTooBusy()) {
			// Jive disconnects clients whenever it encounters an unexpected exception
			throw new ServerTooBusyException();
		}			
		if (!glue.authenticateJabberUser(username, token, digest))
			throw new UnauthorizedException("Not authorized");

		Log.debug("auth succeeded for user " + username);
	}

	public String getPassword(String username) throws UserNotFoundException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void setPassword(String username, String password) throws UserNotFoundException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
		
	}

	public boolean supportsPasswordRetrieval() {
		return false;
	}		
}
	
