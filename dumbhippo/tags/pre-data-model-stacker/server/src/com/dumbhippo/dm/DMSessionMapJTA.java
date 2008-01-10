package com.dumbhippo.dm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.tx.TxUtils;

/**
 * Implementation of DMSessionMap for use within a JTA environment.
 * 
 * @author otaylor
 */
public class DMSessionMapJTA implements DMSessionMap {
	protected static final Logger logger = GlobalSetup.getLogger(DMSessionMapJTA.class);
	
	// We can use a synchronizedMap here since only one thread should be accessing the entry
	// for a particular transaction at once
	private Map<Transaction, DMSession> sessions = Collections.synchronizedMap(new HashMap<Transaction, DMSession>());

	public DMSession getCurrent() {
		Transaction transaction = TxUtils.getCurrentTransaction();
		return sessions.get(transaction);
	}

	public void initCurrent(DMSession session) {
		Transaction transaction = TxUtils.getCurrentTransaction();

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
		TxUtils.runInTransaction(runnable);
	}
}
