package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;

@Local
public interface MessageSender {
	public void sendPostNotification(Person recipient, Post post);
	
	public void sendPostClickedNotification(Post post, User clicker);
}
