package com.dumbhippo.jms;

import javax.jms.Destination;
import javax.jms.ExceptionListener;
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
public class JmsDestination implements ExceptionListener {
	// State of the connection
	private enum State {
		CLOSED,   // Initial or cleanly closed
		FAILURE,  // Shut down after receiving an exception
		IN_OPEN,  // Some thread is trying to open a connection
		OPEN,     // Finished opening
		SHUTDOWN  // Closed and cannot be reopened
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
				sessionFactory = new TxJmsSessionFactory(context, this);
			else
				sessionFactory = new NonTxJmsSessionFactory(context, this);
		} catch (NamingException e) {
			throw new JMSException("Can't create look up connection factory");
		}
	}
	
	private synchronized JmsSessionFactory getOpenSessionFactory() throws JMSException, JmsShutdownException {
		// The idea of the complexity here is that if we are in a failed condition,
		// and lots of threads are trying to produce messages for the same destination
		// we don't want to fill the logs with messages, so we set things up
		// the first thread that tries to use the connection after a failure waits
		// for a time before trying to open the connection, and everybody else
		// waits for that thread to succeed. If that thread instead fails, the
		// next thread proceeds to wait then try to open, and so forth.
		//
		
		while (state != State.OPEN) {
			switch (state) {
			case OPEN: // not reached
				break;
			case SHUTDOWN:
				throw new JmsShutdownException();
			case IN_OPEN:
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while waiting for open");
				}
				break;
			case CLOSED:
			case FAILURE:
				State oldState = state;
				state = State.IN_OPEN;
				State nextState = State.FAILURE;
				
				try {
					if (oldState == State.FAILURE) {
						wait(RETRY_INTERVAL_MILLISECONDS);
						if (state == State.SHUTDOWN) {
						    // shut down while we were sleeping
							nextState = State.SHUTDOWN;
							continue;
						}
					}

					open();
					nextState = State.OPEN;
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while sleeping on retry");
				} finally {
					state = nextState;
					notifyAll();
				}
				break;
			}
		}
		
		return sessionFactory;
	}
	
	public synchronized void close() {
		switch (state) {
		case IN_OPEN:
		case CLOSED:
		case FAILURE:
		case SHUTDOWN:
			break;
		case OPEN:
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
			break;
		}
	}
	
	/**
	 * Close the current connection and mark the object as permanently shut down.
	 * In many cases there is no difference between this and close(), but it's
	 * useful for stopping a JmsConsumer, which would otherwise automatically
	 * reopen the connection. 
	 */
	public synchronized void shutdown() {
		switch (state) {
		case SHUTDOWN:
			break;
		case OPEN:
			close();
			// fall through
		case IN_OPEN:
		case CLOSED:
		case FAILURE:
			state = State.SHUTDOWN;
			break;
		}
	}
	
	public synchronized void closeOnFailure() {
		switch (state) {
		case IN_OPEN:
		case SHUTDOWN:
		case FAILURE:
			break;
		case OPEN:
			close();
			// fall through
		case CLOSED:
			state = State.FAILURE;
			break;
		}
	}

	protected JmsSession createSession() throws JMSException, JmsShutdownException {
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

	public void onException(JMSException e) {
		logger.error("Exception listener caught exception", e);
		closeOnFailure();
	}
}
