/**
 * 
 */
package com.dumbhippo.jive;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.auth.AuthFactory;
import org.jivesoftware.wildfire.auth.UnauthorizedException;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.ServerTooBusyException;

/**
 * @author hp
 *
 */
public class HippoAuthProvider implements
		org.jivesoftware.wildfire.auth.AuthProvider {

	/* (non-Javadoc)
	 * @see org.jivesoftware.wildfire.auth.AuthProvider#isPlainSupported()
	 */
	public boolean isPlainSupported() {
		
		Log.debug("isPlainSupported()");
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.wildfire.auth.AuthProvider#isDigestSupported()
	 */
	public boolean isDigestSupported() {
		
		Log.debug("isDigestSupported()");
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.wildfire.auth.AuthProvider#authenticate(java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String password)
			throws UnauthorizedException {
	
		Log.debug("authenticate() username = " + username + " password = " + password);
		
		throw new UnsupportedOperationException("Plain text passwords are not supported");
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.wildfire.auth.AuthProvider#authenticate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException {
		
		Log.debug("authenticate() username = " + username + " token = " + token + " digest = " + digest);
		
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		
		if (HippoUserProvider.ENABLE_ADMIN_USER && username.equals(HippoUserProvider.getAdminUsername())) {
            String anticipatedDigest = AuthFactory.createDigest(token, HippoUserProvider.getAdminPassword());
			Log.debug("trying to auth admin, expected " + anticipatedDigest);	            
            if (!digest.equalsIgnoreCase(anticipatedDigest)) {
                throw new UnauthorizedException("Bad admin password");
            }
		} else {			
			if (glue.isServerTooBusy()) {
				// Jive disconnects clients whenever it encounters an unexpected exception
				throw new ServerTooBusyException();
			}			
			if (!glue.authenticateJabberUser(username, token, digest))
				throw new UnauthorizedException("Not authorized");
		}
		Log.debug("auth succeeded for user " + username);
	}		
}
	
