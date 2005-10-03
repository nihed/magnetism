package com.dumbhippo.server;

import com.dumbhippo.persistence.ServerSecret;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class AuthenticationSystemBean implements AuthenticationSystem {
	public ServerSecret getServerSecret() {
		SessionWrapper wrapper = Storage.getGlobalPerThreadSession();		
		ServerSecret secret = (ServerSecret) wrapper.getSession().createQuery("from ServerSecret").uniqueResult();
		if (secret == null) {
			secret = new ServerSecret();
			wrapper.getSession().save(secret);
		}
		return secret;
	}
}
