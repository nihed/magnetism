package com.dumbhippo.server.dm;

import java.io.Serializable;

import com.dumbhippo.dm.ClientMatcher;
import com.dumbhippo.dm.DMClient;
import com.dumbhippo.identity20.Guid;

public class UserClientMatcher implements ClientMatcher, Serializable {
	private static final long serialVersionUID = 1L;

	private Guid userId;

	public UserClientMatcher(Guid userId) {
		this.userId = userId;
	}
	
	public boolean matches(DMClient client) {
		if (!(client instanceof UserClient))
			return false;
		
		return ((UserClient)client).getUserId().equals(userId);
	}
	
	@Override
	public String toString() {
		return "{UserClientMatcher userId=" + userId + "}";
	}
}
