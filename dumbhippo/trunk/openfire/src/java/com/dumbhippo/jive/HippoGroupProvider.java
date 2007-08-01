package com.dumbhippo.jive;

import java.util.Collection;

import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupAlreadyExistsException;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.openfire.group.GroupProvider;
import org.xmpp.packet.JID;

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

	public void addMember(String groupName, JID user,
			boolean administrator) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void updateMember(String groupName, JID user,
			boolean administrator) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void deleteMember(String groupName, JID user)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public boolean isReadOnly() {
		return false;
	}

	public Collection<String> getGroupNames() {
		throw new UnsupportedOperationException();		
	}

	public Collection<String> getGroupNames(int startIndex, int numResults) {
		throw new UnsupportedOperationException();		
	}

	public Collection<String> getGroupNames(JID user) {
		throw new UnsupportedOperationException();		
	}

	public boolean isSearchSupported() {
		return false;
	}

	public Collection<String> search(String query) {
		throw new UnsupportedOperationException();		
	}

	public Collection<String> search(String query, int startIndex, int numResults) {
		throw new UnsupportedOperationException();		
	}
}
