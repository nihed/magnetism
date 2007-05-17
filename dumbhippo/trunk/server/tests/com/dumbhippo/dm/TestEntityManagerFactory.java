package com.dumbhippo.dm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;

public class TestEntityManagerFactory implements EntityManagerFactory {
	private EntityManagerFactory delegateFactory;
	private boolean open = true;

	public TestEntityManagerFactory(EntityManagerFactory delegateFactory) {
		this.delegateFactory = delegateFactory;
	}
	
	public void close() {
		open = false;
	}

	public EntityManager createEntityManager() {
		return new TestEntityManager(this);
	}

	public EntityManager createEntityManager(Map properties) {
		return createEntityManager();
	}

	public boolean isOpen() {
		return open;
	}
	
	private Map<Transaction, EntityManager> delegates = Collections.synchronizedMap(new HashMap<Transaction, EntityManager>());
	
	private Transaction getCurrentTransaction() {
		return (((HibernateEntityManagerFactory)delegateFactory).getSessionFactory()).getCurrentSession().getTransaction();
	}
	
	public EntityManager getCurrentDelegate() {
		Transaction transaction = getCurrentTransaction();
		EntityManager delegate = delegates.get(transaction);
		if (delegate == null)
			throw new IllegalStateException("No current transaction, or transaction wasn't begun through test framework");
		
		return delegate;
	}

	public EntityManager createDelegate() {
		EntityManager em = delegateFactory.createEntityManager();
		
		Transaction transaction = ((HibernateEntityManager)em).getSession().getTransaction();

		delegates.put(transaction, em);
		transaction.registerSynchronization(new CleanupSynchronization(transaction));
		
		return em;
	}
	
	private class CleanupSynchronization implements Synchronization {
		private Transaction transaction;

		public CleanupSynchronization(Transaction transaction) {
			this.transaction = transaction;
		}
		
		public void beforeCompletion() {
		}

		public void afterCompletion(int arg0) {
			delegates.remove(transaction);
		}		
	}
}
