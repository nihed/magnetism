package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Synchronization;

import org.hibernate.Session;
import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.dm.TestGroupMemberDMO;
import com.dumbhippo.dm.dm.TestUserDMO;

public class TestSupport {
	TestEntityManagerFactory testEmf;
	EntityManagerFactory delegateEmf;
	static TestSupport instance;
	
	public static synchronized TestSupport getInstance() {
		if (instance == null)
			instance = new TestSupport();
		
		return instance;
	}
	
	private TestSupport() {
		DataModel model = DataModel.getInstance();
		
		delegateEmf = Persistence.createEntityManagerFactory("dmtest");
		testEmf = new TestEntityManagerFactory(delegateEmf);
		model.setSessionMap(new TestSessionMap(delegateEmf));
		model.setEntityManagerFactory(testEmf);
		model.addDMClass(TestUserDMO.class);
		model.addDMClass(TestGroupDMO.class);
		model.addDMClass(TestGroupMemberDMO.class);
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
			ThreadLocalSessionContext.unbind(((HibernateEntityManagerFactory)delegateEmf).getSessionFactory());
		}

		public void beforeCompletion() {
		}
	}
}
