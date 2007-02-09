package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.Set;

import org.jboss.annotation.ejb.Service;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.XmppMessageSenderProvider;
import com.dumbhippo.server.XmppMessageSender;

@Service
public class XmppMessageSenderBean implements XmppMessageSender {
	XmppMessageSenderProvider provider;

	public void sendLocalMessage(Guid to, String payload) {
		sendLocalMessage(Collections.singleton(to), payload);
	}

	public void sendLocalMessage(Set<Guid> to, String payload) {
		if (provider != null)
			provider.sendMessage(to, payload);
	}

	public void sendNewPostMessage(User recipient, Post post) {
		if (provider != null)
			provider.sendNewPostMessage(recipient, post);
	}

	public void setProvider(XmppMessageSenderProvider provider) {
		this.provider = provider;
	}
}
