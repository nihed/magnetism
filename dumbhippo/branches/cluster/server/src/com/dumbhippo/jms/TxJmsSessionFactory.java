package com.dumbhippo.jms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * Version of JmsSessionFactory used in the transactional case, where we share
 * a single underlying session object between all JmsSession objects for the
 * same transaction.
 * 
 * The operation here is similar to the official J2EE way of using JMS from
 * within an EJB container, where a wrapper Session object is created for the
 * Connection, though we offer only the simplified JmsSession interface on
 * the wrapper rather than the full Session interface, rather than the full
 * Session object.  (The J2EE functionality is accessed via the java:/JmsXA 
 * ConnectionFactory within JBoss.)
 * 
 * The advantages of rolling our own are simplicity, the ability to share a
 * connection between multiple threads (that may be intended by the J2EE spec,
 * but, if so, the JBoss implementation doens't match the intention), and
 * simplicity of code by not dealing with all the JCA stuff. The simplicity
 * is very helpful in auditing the retry logic we layer on top with 
 * JmsConsumer and JmsProducer.
 *  
 * @author otaylor
 */
class TxJmsSessionFactory implements JmsSessionFactory {
	protected static final Logger logger = GlobalSetup.getLogger(TxJmsSessionFactory.class);
	
	XAConnection connection;
	// We can use a synchronizedMap here since only one thread should be accessing the entry
	// for a particular transaction at once
	Map<Transaction, XASession> sessions = Collections.synchronizedMap(new HashMap<Transaction, XASession>());
	
	public TxJmsSessionFactory(Context namingContext) throws NamingException, JMSException {
		XAConnectionFactory connectionFactory;
		
		connectionFactory = (XAConnectionFactory) namingContext.lookup("XAConnectionFactory");
		connection = connectionFactory.createXAConnection();
	}

	/**
	 * Gets the current transaction. Throws a RuntimeException if there is 
	 * no current transaction.
	 * 
	 * (Utility method, here because we need it for JmsDestination.setRollbackOnly()) 
	 * 
	 * @return the current Transaction.
	 */
	Transaction getCurrentTransaction() {
		TransactionManager tm = getTransactionManager();
		Transaction transaction;
		
		try {
			transaction = tm.getTransaction();
		} catch (SystemException e) {
			throw new RuntimeException("Error getting current transaction", e);
		}
		if (transaction == null)
			throw new RuntimeException("No current transaction");
		
		return transaction;
	}
	
	private Session getSessionForTx() throws JMSException {
		Transaction transaction = getCurrentTransaction();
		
		XASession session = sessions.get(transaction);
		if (session != null)
			return session;
		
		session = connection.createXASession();
		sessions.put(transaction, session);
		
		try {
			transaction.enlistResource(session.getXAResource());
			transaction.registerSynchronization(new CleanupSynchronization(transaction, session));
		} catch (RollbackException e) {
			sessions.remove(transaction);
			throw new RuntimeException("Can't enlist JMS session in transaction", e);
		} catch (SystemException e) {
			sessions.remove(transaction);
			throw new RuntimeException("Can't enlist JMS session in transaction", e);
		}
		
		return session;
	}
	
	public JmsSession createSession() throws JMSException {
		return new TxJmsSession(getSessionForTx());
	}
	
	public void close() throws JMSException {
		synchronized(sessions) {
			for (Session session : sessions.values()) {
				try {
					session.close();
				} catch (JMSException e) {
					logger.warn("Error closing JMS Session on close()", e);
				}
			}
			
			sessions.clear();
		}
		
		connection.close();
	}

	private TransactionManager getTransactionManager() {
		try {
			Context context = new InitialContext();
			return (TransactionManager)context.lookup("java:/TransactionManager");
		} catch (NamingException e) {
			throw new RuntimeException("Can't get transaction manager", e);
		}
	}

	private class CleanupSynchronization implements Synchronization {
		private Transaction transaction;
		private Session session;

		public CleanupSynchronization(Transaction transaction, Session session) {
			this.transaction = transaction;
			this.session = session;
		}
		
		public void beforeCompletion() {
		}

		public void afterCompletion(int arg0) {
			sessions.remove(transaction);
			try {
				session.close();
			} catch (JMSException e) {
				logger.warn("Error closing JMS session after transaction completion", e);
			}
		}		
	}
}
