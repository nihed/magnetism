package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;

/**
 * Glue between XmppMessageSender and its implementation within wildfire. 
 */
@Local
public interface XmppMessageSenderProvider {
	public void sendMessage(Set<Guid> to, String payload);
	public void sendNewPostMessage(User recipient, Post post);
}
