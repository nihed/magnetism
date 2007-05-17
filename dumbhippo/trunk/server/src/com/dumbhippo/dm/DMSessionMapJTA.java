package com.dumbhippo.dm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class DMSessionMapJTA implements DMSessionMap {
	// We can use a synchronizedMap here since only one thread should be accessing the entry
	// for a particular transaction at once
	private Map<Transaction, DMSession> sessions = Collections.synchronizedMap(new HashMap<Transaction, DMSession>());

	private TransactionManager getTransactionManager() {
		try {
			Context context = new InitialContext();
			return (TransactionManager)context.lookup("java:/TransactionManager");
		} catch (NamingException e) {
			throw new RuntimeException("Can't get transaction manager", e);
		}
	}

	protected Transaction getCurrentTransaction() {
		TransactionManager tm = getTransactionManager();
		Transaction transaction;
		
		try {
			transaction = tm.getTransaction();
		} catch (SystemException e) {
			throw new RuntimeException("Error getting current transaction", e);
		}
		if (transaction == null)
			throw new IllegalStateException("No current transaction");
		
		return transaction;
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
		
		try {
			transaction.registerSynchronization(new CleanupSynchronization(transaction, session));
		} catch (RollbackException e) {
			sessions.remove(transaction);
			throw new RuntimeException("Can't register DM session in transaction", e);
		} catch (SystemException e) {
			sessions.remove(transaction);
			throw new RuntimeException("Can't register DM session in transaction", e);
		}
	}
	
	private class CleanupSynchronization implements Synchronization {
		private Transaction transaction;
//		private DMSession session;

		public CleanupSynchronization(Transaction transaction, DMSession session) {
			this.transaction = transaction;
//			this.session = session;
		}
		
		public void beforeCompletion() {
		}

		public void afterCompletion(int arg0) {
			sessions.remove(transaction);
			// FIXME: Mark the session as closed
		}		
	}
}
