package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.tx.RetryException;

@Local
public interface MessageSender {
	public void sendPostNotification(Resource recipient, Post post, PostType postType) throws RetryException;
}
