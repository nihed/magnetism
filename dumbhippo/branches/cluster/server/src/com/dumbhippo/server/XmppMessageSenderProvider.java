package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;

/**
 * Glue between XmppMessageSender and its implementation within wildfire. 
 */
@Local
public interface XmppMessageSenderProvider {
	public void sendMessage(Set<Guid> to, String payload);
}
