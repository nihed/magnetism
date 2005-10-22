package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Person;

@Local
public interface MessageSender {
	public void sendShareLink(Person recipient, Guid postGuid, String url, String title);
}