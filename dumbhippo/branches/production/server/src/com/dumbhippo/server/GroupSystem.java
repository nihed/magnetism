package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.GroupMemberView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/**
 * @author walters
 *
 */
@Local
public interface GroupSystem {
	/**
	 * Create a new group
	 * @param creator the User creating the group; the group will be created with only this user as a member
	 * @param name name for the new group
	 * @param access the visibility of the group
	 * @param description initial description for the group, may be null
	 * @return the newly created group
	 */
	public Group createGroup(User creator, String name, GroupAccess access, String description);
	
	/**
	 * Returns a set of groups an invitee was invited to by the adder.
	 * 
	 * @param adder
	 * @param invitee
	 * @return a set of groups an invitee was invited to by the adder
	 */
	public Set<Group> getInvitedToGroups(User adder, Resource invitee);
	
	/**
	 * Returns true if the viewer can see the content of the group's mugshot and stacker.
	 * 
	 * @param viewpoint
	 * @param group
	 * @return true if the viewer can see the content of the group's mugshot and stacker.
	 */
	public boolean canSeeContent(Viewpoint viewpoint, Group group);
	
	/**
	 * This checks if an adder can add members other than self.
	 * Even if the group is public with open membership, you still have to be an active member
	 * to add other people.
	 * 
	 * @param adder
	 * @param group
	 * @return true if an adder can add other people to the group
	 */
	public boolean canAddMembers(User adder, Group group);
	
	public void addMember(User adder, Group group, Resource resource);
	
	public void addMember(User adder, Group group, Person person);
	
	public void reviseGroupMembershipPolicy(User revisor, Group group, boolean open);
	
	public Pair<Integer, Integer> inviteAllFollowers(User adder, Group group);
	
	public boolean canRemoveInvitation(User remover, GroupMember groupMember);
	
	public void removeMember(User remover, Group group, Person person);
	
	public void removeMember(User remover, GroupMember groupMember);
	
	public int getMembersCount(Viewpoint viewpoint, Group group, MembershipStatus status);
	
	public Set<GroupMemberView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, int maxResults, PersonViewExtra... extras);

	public Set<User> getUserMembers(Viewpoint viewpoint, Group group);
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group, MembershipStatus status);
	
	public Set<User> getMembershipChangeRecipients(Group group);
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member) throws NotFoundException;
	
	public GroupMember getGroupMember(Group group, Resource member) throws NotFoundException;
	
	public GroupMember getGroupMember(Group group, Guid resourceId) throws NotFoundException;
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member);	
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status);

	
	/**
	 * For all groups, ensure this user has a single GroupMember that points to the user's Account
	 * rather than an email address or something. Used when adding a new AccountClaim.
	 * @param member the user to fix up
	 */
	public void fixupGroupMemberships(User member);
	
	/**
	 * Increase the version number of the group; increasing the group version means
	 * any cached resources for the group (currently, just the group photo) are
	 * no longer valid and must be reloaded. 
	 * 
	 * You must call this function after the corresponding changes are committed;
	 * calling it first could result in stale versions of the resources being
	 * received and cached.
	 * 
	 * @param group the group to update
	 */
	public void incrementGroupVersion(Group group);

	public Set<GroupView> findGroups(Viewpoint viewpoint, User member);
	
	/**
	 * Find the groups that member is in. The returned GroupView objects
	 * will include information about the user inviting the user to the
	 * group only when the viewpoint is the member's own viewpoint; the
	 * inviter information isn't interesting in other cases, so it's
	 * not worth the expense to retrieve.
	 * 
	 * @param viewpoint the viewpoint of the viewer viewing member
	 * @param member the person being viewed
	 * @return a list of GroupView objects for the groups member is in
	 * @return maxResults if positive, maximum number of groups to return
	 */
	public Set<GroupView> findGroups(Viewpoint viewpoint, User member, MembershipStatus status);
	
	public int findGroupsCount(Viewpoint viewpoint, User member, MembershipStatus status);
	
	/**
	 * Find the groups for a resource. 
	 * 
	 * @param viewpoint
	 * @param resource
	 * @return a list GroupMember objects describing group memberships of a resource
	 */
	public List<GroupMember> findGroups(Viewpoint viewpoint, Resource resource);
	
	public void pagePublicGroups(Viewpoint viewpoint, Pageable<GroupView> pageable);
		
	public int getPublicGroupCount();
		
	public Group lookupGroupById(Viewpoint viewpoint, String groupId) throws NotFoundException;
	
	public Group lookupGroupById(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public GroupView loadGroup(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public GroupView getGroupView(Viewpoint viewpoint, Group group);
	
	/**
	 * Finds the set of contacts of an account owner that aren't already
	 * members of a group (and thus can be added to the group)
	 * 
	 * @param viewpoint viewpoint from which we are viewing the setup
	 * @param owner account owner (must equal viewpoint.getViewer() currently)
	 * @param groupId group ID of a group
	 * @param extras info to put in each PersonView
	 * @return the contacts of owner that aren't already members of the group
	 */
	public Set<PersonView> findAddableContacts(UserViewpoint viewpoint, User owner, Group group, PersonViewExtra... extras);
	
	/**
	 * Checks whether the given viewpoint is allowed to change settings of a group
	 * such as the group's name and description.
	 * @param viewpoint viewpoint of a user who wants to edit the group
	 * @param group the group to edit
	 * @return true if the user of the viewpoint is allowed to edit group settings
	 */
	public boolean canEditGroup(UserViewpoint viewpoint, Group group);

	/**
	 * Change the stock photo for a group.
	 * 
	 * @param viewpoint the viewpoint of the user changing the group
	 * @param group the group to change
	 * @param photo the relative path to the photo, or null to unset any previously set photo
	 *   (you have to unset the stock photo to use a custom photo)
	 */
	public void setStockPhoto(UserViewpoint viewpoint, Group group, String photo);

	/**
	 * Transition from INVITED->MEMBER or INVITED_TO_FOLLOW->FOLLOWER.
	 * 
	 * @param userView current viewpoint
	 * @param group group for which we accept invitation
	 */
	public void acceptInvitation(UserViewpoint userView, Group group);

	/**
	 * Search the database of groups using Lucene.
	 * 
	 * @param viewpoint the viewpoint being searched from
	 * @param queryString the search string to use, in Lucene syntax. The search
	 *   will be done across both the group name and description fields
	 * @return a GroupSearchResult object representing the search; you should
	 *    check the getError() method of this object to determine if an error
	 *    occurred (such as an error parsing the query string) 
	 */
	public GroupSearchResult searchGroups(Viewpoint viewpoint, String queryString);
	
	/**
	 * Get a range of groups from the result object returned from searchGroups(). 
	 * This is slightly more efficient than calling GroupSearchResult getGroups(),
	 * because we avoid some EJB overhead.
	 * 
	 * @param viewpoint the viewpoint for the returned GroupView objects; must be the same 
	 *        as the viewpoint passed in when calling searchGroups()
	 * @param searchResult the result
	 * @param start the index of the first group to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of GroupView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<GroupView> getGroupSearchGroups(Viewpoint viewpoint, GroupSearchResult searchResult, int start, int count);

	/**
	 * Return a list of all group guids.
	 * 
	 * @return list of group guids
	 */
	public List<Guid> getAllGroupIds();
}
