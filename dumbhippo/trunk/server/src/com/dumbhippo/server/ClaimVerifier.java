package com.dumbhippo.server;

import javax.ejb.Local;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.User;

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
	 * @param user user who will own the resource
	 * @param resource resource to be owned
	 * @return new auth key to prove this claim
	 */
	public String getAuthKey(User user, Resource resource); 
	
	/**
	 * Try to verify ownership of a resource. If resource is null, then 
	 * resource is expected to be in the existing ResourceClaimToken.
	 * If resource is non-null, the ResourceClaimToken is expected to 
	 * have a null Resource or matching resource.
	 * The logged-in user has to match the user in the ResourceClaimToken,
	 * if there is a logged-in user; sometimes we get the auth key from 
	 * the AIM bot or something.
	 * If this method succeeds then a new AccountClaim is created.
	 * 
	 * @param user logged-in user for double-check or null if unknown
	 * @param token token for verification
	 * @param resource resource, or null
	 * @throws ClaimVerifierException if no ownership claim is created
	 */
	public void verify(User user, ResourceClaimToken token, Resource resource) throws HumanVisibleException;
		
	/**
	 * Send email or an IM with a link that, when clicked, will verify the association of the given user 
	 * with the given address.
	 * 
	 * @param viewpoint viewpoint we're signed in as
	 * @param user user to change
	 * @param address the email or AIM address
	 * @throws HumanVisibleException any error to display
	 */
	public void sendClaimVerifierLink(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException;
}
