package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface PersonViewer {
	
	/** 
	 * Get the contacts of the given person as a list of PersonView
	 * @param viewpoint viewpoint person viewing the contacts (only
	 *          a user can see their contacts, so if viewpoint.getviewer()
	 *          doesn't match user, the result will be empty)    
	 * @param user who to get contacts of
	 * @param includeSelf whether to include the user in the list
	 * @param extras info to stuff into the PersonView objects
	 * @return their contacts
	 */
	public Set<PersonView> getContacts(Viewpoint viewpoint, User user, boolean includeSelf, PersonViewExtra... extras);
	
	/**
	 * Get a list of users who have this user as a contact, but who are not contacts of this user.
	 * An empty list will be returned, if the viewpoint is anything other than the viewpoint of the 
	 * user.
	 * 
	 * @param viewpoint
	 * @param user a user we are getting followers for
	 * @param extras
	 * @return a list of followers for the user
	 */
	public Set<PersonView> getFollowers(Viewpoint viewpoint, User user, PersonViewExtra... extras);
	
	/**
	 * Returns an object describing a person from the viewpoint of another person.
	 * 
	 * @param viewpoint the viewpoint of the person who is viewing
	 * @param p the person being viewed
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return a new PersonView object
	 */
	public PersonView getPersonView(Viewpoint viewpoint, Person p, PersonViewExtra... extras);
	
	/**
	 * Returns an object describing a person from the viewpoint of another person,
	 * given a resource owned by the person. If a Contact or User exists for the 
	 * resource, the resource is not treated specially; it's just a "search handle."
	 * If no Contact or User exists, then the PersonView will contain *only* the
	 * passed-in resource. This latter case is why at least one PersonViewExtra is required,
	 * so the PersonView is guaranteed to have some information in it. Otherwise
	 * no methods on the PersonView returned by this method would be safe to call.
	 * 
	 * FIXME A better approach might be that PersonView can have an implicit extra
	 * "NAME" which is always available, and for a resource-only PersonView we 
	 * return the empty set / null for getResources(), getPrimaryResource() but still
	 * have a working getName() where the name is resource.getHumanReadableString().
	 * The thing to avoid is that a different set of PersonView methods is valid depending
	 * on whether a Resource passed to getPersonView() has associated Contact/User, since
	 * that creates corner-case-only bugs.
	 * 
	 * @param viewpoint the viewpoint of the person who is viewing
	 * @param resource the person being viewed
	 * @param firstExtra at least one extra is mandatory when creating a resource
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return a new PersonView object
	 */
	public PersonView getPersonView(Viewpoint viewpoint, Resource resource, PersonViewExtra firstExtra, PersonViewExtra... extras);
	
	/**
	 * 
	 * Returns an object describing a person from the global viewpoint.
	 * 
	 * @param user the person being viewed. Always an account holder
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return new PersonView object
	 */
	public PersonView getSystemView(User user, PersonViewExtra... extras);
	
	/**
	 * Get all users on the system (admin users only)
	 * 
	 * @return an unsorted set of all users on the system, as PersonView
	 *   objects from the omniscient System viewpoint.
	 */  
	 public Set<PersonView> getAllUsers(UserViewpoint viewpoint);
}
