package com.dumbhippo.server;

import javax.ejb.Local;

@Local
public interface MessageSender {
	public void sendShareLink(String recipient, String url, String title);
}