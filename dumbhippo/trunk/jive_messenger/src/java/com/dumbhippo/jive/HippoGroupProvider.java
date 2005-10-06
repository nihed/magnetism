package com.dumbhippo.jive;

import java.util.Collection;

import org.jivesoftware.messenger.group.Group;
import org.jivesoftware.messenger.group.GroupAlreadyExistsException;
import org.jivesoftware.messenger.group.GroupNotFoundException;
import org.jivesoftware.messenger.group.GroupProvider;
import org.jivesoftware.messenger.user.User;

public class HippoGroupProvider implements GroupProvider {

	public Group createGroup(String name) throws UnsupportedOperationException,
			GroupAlreadyExistsException {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteGroup(String name) throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public Group getGroup(String name) throws GroupNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setName(String oldName, String newName)
			throws UnsupportedOperationException, GroupAlreadyExistsException {
		// TODO Auto-generated method stub

	}

	public void setDescription(String name, String description)
			throws GroupNotFoundException {
		// TODO Auto-generated method stub

	}

	public int getGroupCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Collection<Group> getGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Group> getGroups(int startIndex, int numResults) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Group> getGroups(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addMember(String groupName, String username,
			boolean administrator) throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public void updateMember(String groupName, String username,
			boolean administrator) throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public void deleteMember(String groupName, String username)
			throws UnsupportedOperationException {
		// TODO Auto-generated method stub

	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

}
