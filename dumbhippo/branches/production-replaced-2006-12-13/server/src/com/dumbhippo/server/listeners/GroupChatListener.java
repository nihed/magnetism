package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.GroupMessage;

public interface GroupChatListener {
	public void onGroupMessageCreated(GroupMessage message);
}
