package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface MessageSender {
	public void sendPostNotification(Resource recipient, Post post, PostType postType);
	
	public void sendMySpaceNameChangedNotification(User user);
	
	public void sendMySpaceContactCommentNotification(User user);

	public void sendPrefChanged(User user, String key, String value);
}
