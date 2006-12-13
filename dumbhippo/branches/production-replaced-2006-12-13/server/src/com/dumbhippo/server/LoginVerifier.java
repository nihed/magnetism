package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Token;

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
	 * @throws HumanVisibleException if resource isn't associated with a user
	 */
	public LoginToken getLoginToken(Resource resource) throws HumanVisibleException;
	
	/**
	 * Try to sign in a login or previously-viewed invitation token, returning the person 
	 * you have successfully logged in as and a new cookie, or throwing an exception if 
	 * something goes wrong.
	 * 
	 * @param token token for verification
	 * @param clientName a name for the client cookie
	 * @returns new cookie
	 * @throws HumanVisibleException if no ownership claim is created
	 */
	public Client signIn(Token token, String clientName) throws HumanVisibleException;
	
}
