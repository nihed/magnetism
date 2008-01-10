package com.dumbhippo.server.views;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.ChatMessageDMO;
import com.dumbhippo.server.dm.ChatMessageKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.FeedDMO;

/**
 * The Viewpoint class represents the concept of "current user". 
 * UserViewpoint is the normal case where we have a user authenticated
 * to the system, but there is also AnonymousViewpoint, when no
 * user is logged in, and SystemViewpoint, representing the omniscient
 * (and omnipotent) system view.
 * 
 * While the name Viewpoint implies the class is about viewing the
 * database, not about modification, the concept is more general
 * and can apply to checks on database modification as well as
 * read-only acccess.
 * 
 * One of the primary uses of Viewpoint is to make better use of
 * static type checking; instead of of passing two User to a function
 * and risk mixing up the logged in User with some other User that
 * is being acted upon, we can pass in a UserViewpoint and a User
 * and make the distinction clear. The distinction between Viewpoint
 * and UserViewpoint also allows us to clearly distinguish functions
 * where we can take any sort of Viewpoint, or where we need a logged-in
 * user.
 * 
 * Viewpoint can also be used to cache information about the viewer with request 
 * scope for the cache (i.e. a new Viewpoint has to be created per-request
 * and ideally we only create one per request)
 * 
 * @author otaylor
 */
public abstract class Viewpoint implements DMViewpoint {
	/**
	 * Checks to see if the Viewpoint is the Viewpoint of a particular
	 * user. This is useful because we frequently handle the case where
	 * a user is viewing or modifying their own data.
	 * 
	 * @param user a User object 
	 * @return true if the Viewpoint is a UserViewpoint for the user
	 */
	public abstract boolean isOfUser(User user);
	
	public abstract boolean canSeeFriendsOnly(Guid userId);
	public abstract boolean canSeePrivate(Guid userId);
	public abstract boolean canSeeBlock(BlockDMOKey blockKey);
	public abstract boolean canSeeContact(Guid contactId);
	public abstract boolean canSeeGroup(Guid groupId);
	public abstract boolean canSeePost(Guid postId);
	
	public abstract Site getSite();
	
	/**
	 * converts the viewpoint to an anonymous viewpoint, preserving the 
	 * Site and any other future non-user-specific info we might add.
	 * 
	 * @return
	 */
	public AnonymousViewpoint anonymize() {
		return AnonymousViewpoint.getInstance(getSite());
	}
	
	public boolean canSeeChatMessage(ChatMessageKey chatMessageKey) {
		DMSession session = DataService.getModel().currentSession();

		StoreKey<?, ?> delegateKey;
		try {
			delegateKey = (StoreKey<?,?>)session.getRawProperty(ChatMessageDMO.class, chatMessageKey, "visibilityDelegate");
			return delegateKey.isVisible(this);
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	public boolean canSeeFeed(Guid guid) {
		DMSession session = DataService.getModel().currentSession();

		StoreKey<?, ?> delegateKey;
		try {
			delegateKey = (StoreKey<?,?>)session.getRawProperty(FeedDMO.class, guid, "visibilityDelegate");
			return delegateKey.isVisible(this);
		} catch (NotFoundException e) {
			return false;
		}
	}
}
