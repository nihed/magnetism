package com.dumbhippo.server.impl;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;

/**
 * This class is a little holder for the case where we want
 * to query a Group, and if it exists, a GroupMember out of
 * the database; with EJB QL, you can only return one object,
 * but that can be NEW GroupQueryResult(group, GroupMember).
 * 
 * @author otaylor
 */
public class GroupQueryResult {
	private Group group;
	private GroupMember groupMember;
		
	public GroupQueryResult(Group g, GroupMember gm) {
		group = g;
		groupMember = gm;
	}
	
	public Group getGroup() {
		return group;
	}
	
	public GroupMember getGroupMember() {
		return groupMember;
	}
}
