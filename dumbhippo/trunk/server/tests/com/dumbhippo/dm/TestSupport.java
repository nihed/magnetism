package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Synchronization;

import org.hibernate.Session;
import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import com.dumbhippo.dm.dm.TestBlogEntryDMO;
import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.dm.TestGroupMemberDMO;
import com.dumbhippo.dm.dm.TestUserDMO;

public class TestSupport {
	TestEntityManagerFactory testEmf;
	EntityManagerFactory delegateEmf;
	DataModel model;
	static TestSupport instance;
	
	public static synchronized TestSupport getInstance() {
		if (instance == null)
			instance = new TestSupport();
		
		return instance;
	}
	
	private TestSupport() {
		
		delegateEmf = Persistence.createEntityManagerFactory("dmtest");
		testEmf = new TestEntityManagerFactory(delegateEmf);
		model = new DataModel("http://mugshot.org",
							  new TestSessionMap(delegateEmf),
							  testEmf,
							  null,
							  TestViewpoint.class,
							  new TestViewpoint(null));
		model.addDMClass(TestUserDMO.class);
		model.addDMClass(TestGroupDMO.class);
		model.addDMClass(TestGroupMemberDMO.class);
		model.addDMClass(TestBlogEntryDMO.class);
		model.completeDMClasses();
	}
	
	public EntityManager beginTransaction() {
		EntityManager em = testEmf.createDelegate(); 
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
		
		model.initializeReadWriteSession(viewpoint);
		
		return em;
	}
	
	public EntityManager beginSessionRO(DMViewpoint viewpoint) {
		EntityManager em = beginTransaction();
		
		model.initializeReadOnlySession(viewpoint);
		
		return em;
	}

	public EntityManager beginSessionRO(TestDMClient client) {
		EntityManager em = beginTransaction();
		
		model.initializeReadOnlySession(client);
		
		return em;
	}
	
	public class CleanupSynchronization implements Synchronization {
		public void afterCompletion(int status) {
			ThreadLocalSessionContext.unbind(((HibernateEntityManagerFactory)delegateEmf).getSessionFactory());
		}

		public void beforeCompletion() {
		}
	}
	
	public DataModel getModel() {
		return model;
	}

	public ReadOnlySession currentSessionRO() {
		return model.currentSessionRO();
	}
	
	public ReadWriteSession currentSessionRW() {
		return model.currentSessionRW();
	}
}
