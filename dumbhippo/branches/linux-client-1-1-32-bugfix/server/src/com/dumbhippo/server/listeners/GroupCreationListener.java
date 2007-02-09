package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Group;

public interface GroupCreationListener {
	public void onGroupCreated(Group group);
}
