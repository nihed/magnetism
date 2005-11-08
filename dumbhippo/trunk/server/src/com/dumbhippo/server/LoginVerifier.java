package com.dumbhippo.server;

import javax.ejb.Local;

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
	 * Try to verify a login token, returning the person you have successfully 
	 * logged in as or throwing an exception.
	 * 
	 * @param authKey token for verification
	 * @throws LoginVerifierException if no ownership claim is created
	 */
	public Person verify(String authKey) throws LoginVerifierException;
}
