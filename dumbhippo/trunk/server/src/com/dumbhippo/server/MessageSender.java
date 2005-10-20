package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Person;

@Local
public interface MessageSender {
	public void sendShareLink(Person recipient, String url, String title);
}