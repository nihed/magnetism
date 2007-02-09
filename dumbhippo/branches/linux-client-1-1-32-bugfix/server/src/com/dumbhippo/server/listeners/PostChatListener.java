package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.PostMessage;

public interface PostChatListener {
	public void onPostMessageCreated(PostMessage message);
}
