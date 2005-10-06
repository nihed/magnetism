package com.dumbhippo.jive;

import java.util.Iterator;

import org.jivesoftware.messenger.roster.RosterItem;
import org.jivesoftware.messenger.roster.RosterItemProvider;
import org.jivesoftware.messenger.user.UserAlreadyExistsException;
import org.jivesoftware.messenger.user.UserNotFoundException;

public class HippoRosterItemProvider implements RosterItemProvider {

	public RosterItem createItem(String username, RosterItem item)
			throws UserAlreadyExistsException {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateItem(String username, RosterItem item)
			throws UserNotFoundException {
		// TODO Auto-generated method stub

	}

	public void deleteItem(String username, long rosterItemID) {
		// TODO Auto-generated method stub

	}

	public Iterator<String> getUsernames(String jid) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getItemCount(String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Iterator<RosterItem> getItems(String username) {
		// TODO Auto-generated method stub
		return null;
	}

}
