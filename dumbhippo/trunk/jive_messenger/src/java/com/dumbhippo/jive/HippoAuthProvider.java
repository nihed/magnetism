/**
 * 
 */
package com.dumbhippo.jive;

import org.jivesoftware.messenger.auth.AuthFactory;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

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
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#isDigestSupported()
	 */
	public boolean isDigestSupported() {
		
		Log.debug("isDigestSupported()");
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String password)
			throws UnauthorizedException {
	
		Log.debug("authenticate() username = " + username + " password = " + password);
		
		throw new UnsupportedOperationException("Plain text passwords are not supported");
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException {
		
		Log.debug("authenticate() username = " + username + " token = " + token + " digest = " + digest);
		
		if (HippoUserProvider.ENABLE_ADMIN_USER) {
			if (username.equals(HippoUserProvider.getAdminUsername())) {
				// FIXME this is not a secure password; the idea is that 
				// in a production build the admin user is disabled...
	            String anticipatedDigest = AuthFactory.createDigest(token, HippoUserProvider.getAdminPassword());
				Log.debug("trying to auth admin, expected " + anticipatedDigest);	            
	            if (!digest.equalsIgnoreCase(anticipatedDigest)) {
	                throw new UnauthorizedException("Bad admin password");
	            }
			}
		} else {
			MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);			
			if (glue.authenticateJabberUser(username, token, digest))
				throw new UnauthorizedException("Not authorized");
		}
		Log.debug("auth succeeded for user " + username);
	}		
}
	
