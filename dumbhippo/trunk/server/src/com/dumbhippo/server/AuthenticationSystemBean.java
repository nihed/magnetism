package com.dumbhippo.server;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.ServerSecret;

@Stateless
public class AuthenticationSystemBean implements AuthenticationSystem {
	
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
}
