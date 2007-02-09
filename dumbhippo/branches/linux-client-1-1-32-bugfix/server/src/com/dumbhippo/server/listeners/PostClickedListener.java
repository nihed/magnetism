package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;

public interface PostClickedListener {
	public void onPostClicked(Post post, User user, long clickedTime);
}
