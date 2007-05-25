package com.dumbhippo.dm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.transaction.Synchronization;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ejb.HibernateEntityManagerFactory;

public class TestSessionMap implements DMSessionMap {
	private Map<Transaction, DMSession> sessions = Collections.synchronizedMap(new HashMap<Transaction, DMSession>());
	private SessionFactory sessionFactory;
	
	TestSessionMap(EntityManagerFactory emf) {
		sessionFactory = ((HibernateEntityManagerFactory)emf).getSessionFactory();
	}

	private Transaction getCurrentTransaction() {
		return sessionFactory.getCurrentSession().getTransaction();
	}
	
	public DMSession getCurrent() {
		Transaction transaction = getCurrentTransaction();
		return sessions.get(transaction);
	}

	public void initCurrent(DMSession session) {
		Transaction transaction = getCurrentTransaction();
		
		if (sessions.get(transaction) != null)
			throw new IllegalStateException("DM session already initialized");
		
		sessions.put(transaction, session);
		transaction.registerSynchronization(new CleanupSynchronization(transaction, session));
	}
	
	private class CleanupSynchronization implements Synchronization {
		private Transaction transaction;
		private DMSession session;

		public CleanupSynchronization(Transaction transaction, DMSession session) {
			this.transaction = transaction;
			this.session = session;
		}
		
		public void beforeCompletion() {
		}

		public void afterCompletion(int status) {
			sessions.remove(transaction);
			session.afterCompletion(status);
		}		
	}

	public void runInTransaction(Runnable runnable) {
		EntityManager em = TestSupport.getInstance().beginTransaction();
		
		try {
			runnable.run();
		} catch (RuntimeException e) {
			em.getTransaction().setRollbackOnly();
			throw new RuntimeException("Error in transaction", e);
		} finally {
			try {
				em.getTransaction().commit();
			} catch (RollbackException e) {
				// Presumably because we set rollback-only above
			}
		}
	}
}
