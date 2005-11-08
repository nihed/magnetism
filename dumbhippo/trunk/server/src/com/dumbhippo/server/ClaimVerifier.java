package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;

/**
 * Methods related to adding a new resource to your account.
 * 
 * @author hp
 *
 */
@Local
public interface ClaimVerifier {
	/**
	 * Get or create a new verifier for the given resource. If you pass 
	 * null for the resource, the resource should be provided at verify
	 * time and the auth key can be used to verify any resource we get 
	 * it from, e.g. if we get the auth key from an AIM address then
	 * we verify that AIM address.
	 * 
	 * @param person person who will own the resource
	 * @param resource resource to be owned
	 * @return new auth key to prove this claim
	 */
	public String getAuthKey(Person person, Resource resource); 
	
	/**
	 * Try to verify ownership of a resource. If resource is null, then 
	 * resource is expected to be in the existing ResourceClaimToken.
	 * If resource is non-null, the ResourceClaimToken is expected to 
	 * have a null Resource or matching resource.
	 * The logged-in user has to match the user in the ResourceClaimToken,
	 * if there is a logged-in user; sometimes we get the auth key from 
	 * the AIM bot or something.
	 * If this method succeeds then a new ResourceOwnershipClaim is created.
	 * 
	 * @param user logged-in user for double-check or null if unknown
	 * @param authKey token for verification
	 * @param resource resource, or null
	 * @throws ClaimVerifierException if no ownership claim is created
	 */
	public void verify(Person user, String authKey, Resource resource) throws ClaimVerifierException;
}
