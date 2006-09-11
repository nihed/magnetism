package com.dumbhippo.jms;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.util.EJBUtil;

/**
 * A base class for JmsProducer and JmsConsumer which handles connection and session management. 
 */
public class JmsDestination {
	// State of the connection
	private enum State {
		CLOSED,   // Initial or cleanly shut down
		FAILURE,  // Shut down after receiving an exception
		IN_OPEN,  // Some thread is trying to open a connection
		OPEN      // Finished opening
	}
	
	private static final Logger logger = GlobalSetup.getLogger(JmsDestination.class);
	
	protected static final int RETRY_INTERVAL_MILLISECONDS = 10000;
	protected static final int MAX_RETRIES = 10;
	
	State state = State.CLOSED;
	
	String destinationName;
	private JmsSessionFactory sessionFactory;
	private Destination destination;
	private JmsConnectionType connectionType;
	
	public JmsDestination(String destinationName, JmsConnectionType connectionType) {
		this.destinationName = destinationName;
		this.connectionType = connectionType;
	}
	
	private void open() throws JMSException {
		Context context;
		try {
			if (connectionType.isInServer())
				context = EJBUtil.getHAContext();
			else
				context = new InitialContext();
		} catch (NamingException e) {
			throw new RuntimeException("Can't get context for naming lookups", e);
		}
			
		try {
			destination = (Destination)context.lookup(destinationName);
		} catch (NamingException e) {
			throw new InvalidDestinationException("Can't look up destination '" + destinationName+ "'from JMS");
		}

		try {
			if (connectionType.isTransacted())
				sessionFactory = new TxJmsSessionFactory(context);
			else
				sessionFactory = new NonTxJmsSessionFactory(context);
		} catch (NamingException e) {
			throw new JMSException("Can't create look up connection factory");
		}
	}
	
	private synchronized JmsSessionFactory getOpenSessionFactory() throws JMSException {
		// The idea of the complexity here is that if we are in a failed condition,
		// and lots of threads are trying to produce messages for the same destination
		// we don't want to fill the logs with messages, so we set things up
		// the first thread that tries to use the connection after a failure waits
		// for a time before trying to open the connection, and everybody else
		// waits for that thread to succeed. If that thread instead fails, the
		// next thread proceeds to wait then try to open, and so forth. 
		
		while (state != State.OPEN) {
			if (state == State.IN_OPEN) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while waiting for open");
				}
			} else {
				State oldState = state;
				state = State.IN_OPEN;
				State nextState = State.FAILURE;
				
				try {
					if (oldState == State.FAILURE)
						Thread.sleep(RETRY_INTERVAL_MILLISECONDS);

					open();
					nextState = State.OPEN;
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while sleeping on retry");
				} finally {
					state = nextState;
					notifyAll();
				}
			}
		}
		
		return sessionFactory;
	}
	
	public synchronized void close() {
		destination = null;
		if (sessionFactory != null) {
			try {
					sessionFactory.close();
			} catch (JMSException e) {
				logger.warn("Got error closing session factory", e);
			} finally {
				sessionFactory = null;
			}
		}
		state = State.CLOSED;
	}
	
	public synchronized void closeOnFailure() {
		close();
		state = State.FAILURE;
		
	}

	protected JmsSession createSession() throws JMSException {
		// Some other thread could possibly close the session factory before
		// we call createSession() if it receives an error. In that case,
		// we'll throw a JMSException and the caller will retry.
		return getOpenSessionFactory().createSession();
	}
	
	protected Destination getDestination() {
		return destination;
	}
	
	protected void setRollbackOnly() {
		if (connectionType.isTransacted()) {
			try {
				((TxJmsSessionFactory)sessionFactory).getCurrentTransaction().setRollbackOnly();
			} catch (SystemException e) {
				logger.error("Error marking transaction rollback-only", e);
			}
		}
	}
}
