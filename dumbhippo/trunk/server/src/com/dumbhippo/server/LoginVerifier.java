package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Person;
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
	 * Get or create a new verifier for the given resource.
	 * 
	 * @param resource resource to be proven
	 * @return new auth key to prove resource ownership
	 */
	public String getAuthKey(Resource resource); 
	
	/**
	 * Try to sign in a login token, returning the person you have successfully 
	 * logged in as and a new cookie, or throwing an exception if something
	 * goes wrong.
	 * 
	 * @param token token for verification
	 * @param clientName a name for the client cookie
	 * @returns new cookie and logged-in user
	 * @throws LoginVerifierException if no ownership claim is created
	 */
	public Pair<Client,Person> signIn(LoginToken token, String clientName) throws LoginVerifierException;
}
