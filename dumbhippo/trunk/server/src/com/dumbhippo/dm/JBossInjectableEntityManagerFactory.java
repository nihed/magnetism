package com.dumbhippo.dm;

import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.ejb3.entity.InjectedEntityManagerFactory;

/**
 * JBoss has a class called InjectedEntityManagerFactory that you can bind into
 * JNDI by specifying the jboss.entity.manager.factory.jndi.name for your persistence
 * factory. Unfortunately, if you use the standard createEntityManager() method on
 * it, you get a new raw entity manager, not a transaction-scoped entity manager
 * factory. Despite the fact that a DMO is bound to a particular session, we 
 * need a transaction-scoped entity manager factory because we need to be on the 
 * same page about the entity manager as the session beans.
 * 
 * So this class translates the JBoss InjectedEntityManagerFactory into our idea
 * of a "injectable entity manager" factory  ... a entity manager factory that creates 
 * transaction-scoped entity managers. 
 * 
 * @author otaylor
 */
public class JBossInjectableEntityManagerFactory implements EntityManagerFactory {
	private InjectedEntityManagerFactory factory;
	private boolean closed = false;
	
	public JBossInjectableEntityManagerFactory(String jndiName) {
		try {
			Context context = new InitialContext();
			factory = (InjectedEntityManagerFactory)context.lookup(jndiName);
		} catch (NamingException e) {
			throw new RuntimeException("Can't get entity manager factory", e);
		}
	}
	
	public void close() {
		closed = true;
	}

	public EntityManager createEntityManager() {
		return factory.getEntityManager();
	}

	// Eclipse wants to warn about no type params on Map, but 
	// adding them (even <?,?>) keeps us from implementing the 
	// interface method apparently
	@SuppressWarnings("unchecked")
	public EntityManager createEntityManager(Map arg0) {
		return factory.getEntityManager();
	}

	public boolean isOpen() {
		return !closed;
	}

}
