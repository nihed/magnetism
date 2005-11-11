package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface GroupSystem {
	public Group createGroup(User creator, String name);
	
	public void deleteGroup(User deleter, Group group);
	
	public void addMember(User adder, Group group, Person person);
	
	public void removeMember(User remover, Group group, Person person);
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group);
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status);
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member);
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, Person member);	
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, Person member, MembershipStatus status);	

	/**
	 * Find the groups that member is in. Currently, only the case
	 * where member == viewpoint.getViewer() is implemented; if you
	 * need the more general case, use findRawGroups(). (That doesn't
	 * give the inviter information, but generally when you are viewing
	 * someone else's groups, you don't want that.)
	 * 
	 * @param viewpoint the viewpoint of the viewer viewing member
	 * @param member the person being viewed
	 * @return a list of GroupView objects for the groups member is in
	 */
	public Set<GroupView> findGroups(Viewpoint viewpoint, Person member);	
	
	public Group lookupGroupById(Viewpoint viewpoint, String groupId);
	
	/**
	 * Finds the set of contacts of an account owner that aren't already
	 * members of a group (and thus can be added to the group)
	 * 
	 * @param viewpoint viewpoint from which we are viewing the setup
	 * @param owner account owner (must equal viewpoint.getViewer() currently)
	 * @param groupId group ID of a group
	 * @return the contacts of owner that aren't already members of the group
	 */
	public Set<PersonView> findAddableContacts(Viewpoint viewpoint, User owner, String groupId);
}
