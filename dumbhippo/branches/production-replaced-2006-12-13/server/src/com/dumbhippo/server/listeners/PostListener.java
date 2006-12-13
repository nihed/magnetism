package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Post;

public interface PostListener {
	public void onPostCreated(Post post);
	public void onPostDisabledToggled(Post post);
}
