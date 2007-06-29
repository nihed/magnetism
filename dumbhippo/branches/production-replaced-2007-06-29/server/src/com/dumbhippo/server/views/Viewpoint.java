package com.dumbhippo.server.views;

import com.dumbhippo.persistence.User;

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
public abstract class Viewpoint {
	/**
	 * Checks to see if the Viewpoint is the Viewpoint of a particular
	 * user. This is useful because we frequently handle the case where
	 * a user is viewing or modifying their own data.
	 * 
	 * @param user a User object 
	 * @return true if the Viewpoint is a UserViewpoint for the user
	 */
	public abstract boolean isOfUser(User user);
}
