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
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void deleteGroup(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public Group getGroup(String name) throws GroupNotFoundException {
		throw new GroupNotFoundException();		
		// TODO FIXME
	}

	public void setName(String oldName, String newName)
			throws UnsupportedOperationException, GroupAlreadyExistsException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void setDescription(String name, String description)
			throws GroupNotFoundException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public int getGroupCount() {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public Collection<Group> getGroups() {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public Collection<Group> getGroups(int startIndex, int numResults) {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public Collection<Group> getGroups(User user) {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void addMember(String groupName, String username,
			boolean administrator) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void updateMember(String groupName, String username,
			boolean administrator) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void deleteMember(String groupName, String username)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public boolean isReadOnly() {
		return false;
	}
}
