package com.dumbhippo.server.views;

import java.util.Set;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.ContactDMO;
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
}
