package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;

/**
 * Methods related to logging in by having a token sent to one of your
 * account's owned resources.
 * 
 * @author hp
 *
 */
@Local
public interface LoginVerifier {
	/**
	 * Get or create a new login token to send to the 
	 * given resource to prove the resource is owned.
	 * 
	 * @param resource resource to be proven
	 * @return new token for authentication
	 * @throws LoginVerifierException if resource isn't associated with a user
	 */
	public LoginToken getLoginToken(Resource resource) throws HumanVisibleException;
	
	/**
	 * Try to sign in a login token, returning the person you have successfully 
	 * logged in as and a new cookie, or throwing an exception if something
	 * goes wrong.
	 * 
	 * @param token token for verification
	 * @param clientName a name for the client cookie
	 * @returns new cookie
	 * @throws LoginVerifierException if no ownership claim is created
	 */
	public Client signIn(LoginToken token, String clientName) throws HumanVisibleException;
	
}
