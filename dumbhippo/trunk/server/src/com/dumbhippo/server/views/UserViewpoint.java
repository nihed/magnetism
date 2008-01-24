package com.dumbhippo.server.views;

import java.util.Set;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.ContactDMO;
import com.dumbhippo.server.dm.GroupDMO;
import com.dumbhippo.server.dm.PostDMO;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;

/**
 * UserViewpoint represents the view onto the system and access
 * controls for a logged in user. It is also used as a place to
 * cache data associated with that user's view for the scope
 * of a request. (Keeping the cache request scoped means that 
 * we generally don't need to worry about invalidating the
 * cached data.)
 * 
 * @author otaylor
 */
public class UserViewpoint extends Viewpoint {
	private Guid viewerId;
	private User viewer;
	private Site site;
	private DMSession session;
	
	/**
	 * Creates a UserViewpoint for a new user. In the
	 * normal case, the UserViewpoint is created from the web tier,
	 * and passed down to the rest of the code.
	 * 
	 * @param viewer the user viewing the system
	 */
	public UserViewpoint(User viewer, Site site) {
		if (viewer == null)
			throw new NullPointerException("UserViewpoint created with null user");
		this.viewerId = viewer.getGuid();
		this.viewer = viewer;
		this.site = site;
	}
	
	/**
	 * Allowing creating a UserViewpoint with a viewerId is a convenience for the
	 * case where we want to pass a Viewpoint into a function that will create
	 * it's own transaction, the User object will be created lazily on demand
	 * in this case.
	 * 
	 * @param viewerId
	 */
	public UserViewpoint(Guid viewerId, Site site) {
		if (viewerId == null)
			throw new NullPointerException("UserViewpoint created with null userId");
		this.viewerId = viewerId;
		this.site = site;
	}

	public User getViewer() {
		if (viewer == null) {
			IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
			viewer = spider.lookupUser(viewerId);
		}
		return viewer;
	}
	
	public Guid getViewerId() {
		return viewerId;
	}
	
	@Override
	public Site getSite() {
		return site;
	}
	
	@Override
	public boolean isOfUser(User user) {
		return viewerId.equals(user.getGuid());
	}
	 
	@Override
	public String toString() {
		if (viewer != null)
			return "{UserViewpoint " + viewer + "}";
		else
			return "{UserViewpoint " + viewerId + "}";
	}

	public void setSession(DMSession session) {
		this.session = session;
	}

	@Override
	public boolean canSeeFriendsOnly(Guid userId) {
		if (canSeePrivate(userId))
			return true;
		
		try {
			@SuppressWarnings("unchecked")
			Set<Guid> contacterIds = (Set<Guid>)session.getRawProperty(UserDMO.class, viewerId, "contacters");
			
			return contacterIds.contains(userId);
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean canSeePrivate(Guid userId) {
		return viewerId.equals(userId);
	}
	
	@Override
	public boolean canSeeContact(Guid contactId) {
		try {
			@SuppressWarnings("unchecked")
			Guid contactOwner = (Guid)session.getRawProperty(ContactDMO.class, contactId, "owner");
			
			return contactOwner.equals(getViewerId());
		} catch (NotFoundException e) {
			return false;
		}	
	}
	
	@Override
	public boolean canSeeBlock(BlockDMOKey blockKey) {
		try {
			switch (blockKey.getType().getBlockVisibility()) {
			case PUBLIC:
				return true;
			case OWNER:
				Guid ownerGuid = (Guid)session.getRawProperty(BlockDMO.class, blockKey, "owner");
				return viewerId.equals(ownerGuid);
			case DELEGATE:
				StoreKey<?,?> delegateKey = (StoreKey<?,?>)session.getRawProperty(BlockDMO.class, blockKey, "visibilityDelegate");
				return delegateKey.isVisible(this);
			case NOBODY:
				return false;
			}
		} catch (NotFoundException e) {
			return false;
		}
		
		return false;
	}
	
	@Override
	public boolean canSeeGroup(Guid groupId) {
		try {
			boolean isPublic = (Boolean)session.getRawProperty(GroupDMO.class, groupId, "public");
			if (isPublic)
				return true;
			
			@SuppressWarnings("unchecked")
			Set<Guid> members = (Set<Guid>)session.getRawProperty(GroupDMO.class, groupId, "canSeeMembers");
			return members.contains(viewerId);
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean canSeePost(Guid postId) {
		try {
			boolean isPublic = (Boolean)session.getRawProperty(PostDMO.class, postId, "public");
			if (isPublic)
				return true;
			//  This check is a little too restrictive, in that it doesn't handle
			//
			// A) posts sent to private groups before the viewer joined the group
			// B) posts sent to a resource when someone later claims the resource, since we don't
			//    invalidate the cached expandedRecipients, which are users, resource
			//
			// B) is especially problematical, but is probably best handled by adding the
			// invalidation, rather than trying to cache resources (that is, find all posts
			// where the newly claimed resource is an expandedRecipient, and then invalidate
			// their expandedRecipients)
			
			@SuppressWarnings("unchecked")
			Set<Guid> expandedRecipients = (Set<Guid>)session.getRawProperty(PostDMO.class, postId, "expandedRecipients");
			return expandedRecipients.contains(viewerId);
			
		} catch (NotFoundException e) {
			return false;
		}
	}
}
