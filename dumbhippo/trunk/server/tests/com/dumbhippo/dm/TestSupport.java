package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Synchronization;

import org.hibernate.Session;
import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

public class TestSupport {
	EntityManagerFactory emf;
	static TestSupport instance;
	
	public static synchronized TestSupport getInstance() {
		if (instance == null)
			instance = new TestSupport();
		
		return instance;
	}
	
	private TestSupport() {
		DataModel model = DataModel.getInstance();
		
		emf = Persistence.createEntityManagerFactory("dmtest");
		model.setSessionMap(new TestSessionMap(emf));
		model.setEntityManagerFactory(emf);
		model.addDMClass(TestGroupDMO.class);
	}
	
	public EntityManager beginTransaction() {
		EntityManager em = emf.createEntityManager(); 
		Session session = ((HibernateEntityManager)em).getSession();
		ThreadLocalSessionContext.bind(session);
		
		// If we bind the session to the thread explicitly with bind(), then we have
		// to unbind() it ourself. (Sessions created implicitly are automatically
		// cleaned up on transaction completion.)
		session.getTransaction().registerSynchronization(new CleanupSynchronization());

		em.getTransaction().begin();
		
		return em;
	}

	public EntityManager beginSessionRW(DMViewpoint viewpoint) {
		EntityManager em = beginTransaction();
		
		DataModel.getInstance().initializeReadWriteSession(viewpoint);
		
		return em;
	}
	
	public EntityManager beginSessionRO(DMViewpoint viewpoint) {
		EntityManager em = beginTransaction();
		
		DataModel.getInstance().initializeReadOnlySession(viewpoint);
		
		return em;
	}
	
	public class CleanupSynchronization implements Synchronization {
		public void afterCompletion(int status) {
			ThreadLocalSessionContext.unbind(((HibernateEntityManagerFactory)emf).getSessionFactory());
		}

		public void beforeCompletion() {
		}
	}
}
