package com.dumbhippo.server.impl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.ServerSecret;
import com.dumbhippo.server.AuthenticationSystem;
import com.dumbhippo.server.AuthenticationSystemRemote;

@Stateless
public class AuthenticationSystemBean implements AuthenticationSystem, AuthenticationSystemRemote {
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public ServerSecret getServerSecret() {
		ServerSecret secret;
		try {
			secret = (ServerSecret) em.createQuery("from ServerSecret").getSingleResult();
		} catch (EntityNotFoundException e) {
			secret = new ServerSecret();
			em.persist(secret);
		}
		return secret;
	}

	public boolean authenticateJiveUser(String username, String token, String digest) {
		
		ServerSecret secret = getServerSecret();
		
		// TODO do some stuff here and maybe return true
		
		return false;
	}
}
