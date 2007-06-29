package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.User;

public interface UserCreationListener {
	public void onUserCreated(User user);
}
