package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

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
	 * @throws RetryException 
	 */
	public String getAuthKey(User user, Resource resource) throws RetryException; 
	
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
	 * @param viewpoint logged-in user for double-check or null if unknown
	 * @param token token for verification
	 * @param resource resource, or null
	 * @throws ClaimVerifierException if no ownership claim is created
	 */
	public void verify(Viewpoint viewpoint, ResourceClaimToken token, Resource resource) throws HumanVisibleException;
		
	/**
	 * Send email or an IM with a link that, when clicked, will verify the association of the given user 
	 * with the given address.
	 * 
	 * @param viewpoint viewpoint we're signed in as
	 * @param user user to change
	 * @param address the email address
	 * @throws HumanVisibleException any error to display
	 * @throws RetryException 
	 */
	public void sendClaimVerifierLinkEmail(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException;

	/**
	 * Send email or an IM with a link that, when clicked, will verify the association of the given user 
	 * with the given address.
	 * 
	 * @param viewpoint viewpoint we're signed in as
	 * @param user user to change
	 * @param address the AIM screen name
	 * @throws HumanVisibleException any error to display
	 * @throws RetryException 
	 */
	public void sendClaimVerifierLinkAim(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException;

	/**
	 * Send email or an IM with a link that, when clicked, will verify the association of the given user 
	 * with the given address.
	 * 
	 * @param viewpoint viewpoint we're signed in as
	 * @param user user to change
	 * @param address the XMPP JID
	 * @throws HumanVisibleException any error to display
	 * @throws RetryException 
	 */
	public void sendClaimVerifierLinkXmpp(UserViewpoint viewpoint, User user, String address) throws HumanVisibleException, RetryException;

	/**
	 * Cancel any pending claim tokens for the given user/resource pair
	 * 
	 * @param user user claiming the resource
	 * @param resource the resource being claimed
	 */
	public void cancelClaimToken(User user, Resource resource);
	
	/**
	 * Get a list of resources of a particular type the user has claimed but not yet verified the claim for,
	 *
	 * @param user the user to get the pending claimed resources for
	 * @param klass subclass of resource
	 * @return a list of pending claimed resources
	 */
	public <T extends Resource> List<T> getPendingClaimedResources(User user, Class<T> klass);

	/**
	 * When we first try to verify an XMPP address, we might not yet be subscribed to that
	 * user's presence and thus able to send them messages, so we first send out a presence
	 * subscription request (a friend request, really). When we get the reply, we call
	 * this method to check if there are any claim tokens for the newly-subscribed-to
	 * XMPP address, and, if so, send out the links to that address.
	 * 
	 * @param friendedJid the JID of the local address that has newly been subscribed
	 * @param fromResource the address to which it was subscribed
	 */
	public void sendQueuedXmppLinks(String friendedJid, XmppResource fromResource);
	
}
