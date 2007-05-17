package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.ejb.HibernateEntityManager;

public class TestSupport {
	EntityManagerFactory emf;
	static TestSupport instance;
	
	public static synchronized TestSupport getInstance() {
		if (instance == null)
			instance = new TestSupport();
		
		return instance;
	}
	
	private TestSupport() {
		DMCache cache = DMCache.getInstance();
		
		emf = Persistence.createEntityManagerFactory("dmtest");
		cache.setSessionMap(new TestSessionMap(emf));
		cache.setEntityManagerFactory(emf);
		cache.addDMClass(TestGroupDMO.class);
	}
	
	public EntityManager beginTransaction() {
		EntityManager em = emf.createEntityManager(); 
		em.getTransaction().begin();
		ThreadLocalSessionContext.bind(((HibernateEntityManager)em).getSession());
		
		return em;
	}

	public EntityManager beginSessionRW(DMViewpoint viewpoint) {
		EntityManager em = beginTransaction();
		
		DMCache.getInstance().initializeReadWriteSession(viewpoint);
		
		return em;
	}
	
	public EntityManager beginSessionRO(DMViewpoint viewpoint) {
		EntityManager em = beginTransaction();
		
		em.getTransaction().begin();
		DMCache.getInstance().initializeReadOnlySession(viewpoint);
		
		return em;
	}
}
