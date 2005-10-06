/**
 * 
 */
package com.dumbhippo.jive;

import org.jivesoftware.messenger.auth.UnauthorizedException;

import com.dumbhippo.server.AuthenticationSystemRemote;
import com.dumbhippo.server.client.EjbLink;

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
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#isDigestSupported()
	 */
	public boolean isDigestSupported() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String password)
			throws UnauthorizedException {
		throw new UnsupportedOperationException("Plain text passwords are not supported");
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.messenger.auth.AuthProvider#authenticate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void authenticate(String username, String token, String digest)
			throws UnauthorizedException {
		EjbLink ejb = EjbLink.getInstance();
		
		AuthenticationSystemRemote auth = ejb.getAuthenticationSystem();
		
		if (!auth.authenticateJiveUser(username, token, digest))
			throw new UnauthorizedException("Not authorized");
	}

}
