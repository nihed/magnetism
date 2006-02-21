package com.dumbhippo.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.CommunicationException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * Utility base class that creates a JMS connection, session, and destination.
 * Half the purpose is to hide all the stuupid JMSException that should not be
 * checked exceptions. Also makes communication failures block/retry instead
 * of throwing an exception.
 * 
 * @author hp
 *
 */
public abstract class JmsQueue {

	protected static final Logger logger = GlobalSetup.getLogger(JmsQueue.class);
	private static final int RETRY_INTERVAL_MILLISECONDS = 10000;
	
	private String queue;
	private boolean local;
	
	private Lock initLock;
	private Condition initCondition;
	private Init init;
	
	
	/**
	 * This class is sort of an "init connection transaction," which 
	 * either exists or does not exist in its entirety.
	 * 
	 * @author hp
	 *
	 */
	protected static abstract class Init {
		private Connection connection;
		private Session session;
		private Destination destination;
		
		protected abstract void openSub() throws JMSException, NamingException;
		protected abstract void closeSub() throws JMSException;
		
		Init(String queue, boolean local) throws JMSException, NamingException {
			try {
				InitialContext ctx = new InitialContext();
				
				ConnectionFactory connectionFactory;
				
				if (local)
					connectionFactory = (ConnectionFactory) ctx.lookup("OILConnectionFactory");
				else
					connectionFactory = (ConnectionFactory) ctx.lookup("RMIConnectionFactory");
				
				connection = connectionFactory.createConnection();
				
				// inside an EJB container the args here are supposedly ignored ?
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				
				destination = (Destination) ctx.lookup("queue/" + queue);
				
				openSub();
			} catch (JMSException e) {
				close();
				throw e;
			} catch (NamingException e) {
				close();
				throw e;
			} catch (RuntimeException e) {
				close();
				throw e;
			}
		}
		
		public void close() {
			try {
				if (session != null) {
					//logger.debug("Closing JMS session object");
					session.close();
				}
				if (connection != null) {
					//logger.debug("Closing JMS connection object");
					connection.close();
				}
				closeSub();
			} catch (JMSException e) {
				logger.warn("Exception closing JMS session/connection, ignoring", e);
			} finally {
				// just forget about anything we couldn't close, what else 
				// are we supposed to do?
				connection = null;
				session = null;
				destination = null;
			}
		}
		
		Connection getConnection() {
			return connection;
		}
		
		Session getSession() {
			return session;
		}
		
		Destination getDestination() {
			return destination;
		}
	}
	
	/**
	 * Creates a connection to a JMS queue. The "local" flag 
	 * is intended to correspond roughly to the @Local/@Remote 
	 * interfaces on an EJB, i.e. whether we are inside the app 
	 * server or not.
	 * 
	 * @param queue name of the queue, e.g. "FooQueue"
	 * @param local true if we're inside the app server
	 */
	protected JmsQueue(String queue, boolean local) {
		this.queue = queue;
		this.local = local;
		
		// these protect lazily filling in the connection, etc.
		initLock = new ReentrantLock();
		initCondition = initLock.newCondition();
		//logger.debug("construct JMS queue = " + queue + " local = " + local);
	}
	
	protected Connection getConnection() {
		return open().getConnection();
	}
	
	protected Session getSession() {
		return open().getSession();
	}

	protected Destination getDestination() {
		return open().getDestination();
	}
	
	private void closeAndWaitRetryInterval() {
		logger.warn("JMS init failed, closing and starting over");
		close();
		initLock.lock();
		try {
			// this wakes up on close() or on successful open()
			logger.info("Waiting " + RETRY_INTERVAL_MILLISECONDS + " to reconnect");
			initCondition.await(RETRY_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		} finally {
			initLock.unlock();
		}
	}
	
	/**
	 * Subclasses can bundle more stuff into the Init "transaction"
	 * 
	 * @param queue
	 * @param local
	 * @return a newly-constructed init object
	 * @throws JMSException
	 * @throws NamingException
	 */
	protected abstract Init newInit(String queue, boolean local) throws JMSException, NamingException;
	
	/**
	 * The point of the Init class and all these hoops is to never return null
	 * from open(), so when someone does open().getSession().frobate() that 
	 * will always block until they get a session. frobate() can still throw
	 * JMSException, but there's never a NullPointerException.
	 * 
	 * @return the Init object with all our queue connectivity initialized
	 */
	protected Init open() {
		initLock.lock();
		try {
			while (init == null) {
				try {
					init = newInit(queue, local);
				} catch (CommunicationException e) {
					logger.warn("Failed to communicate with Java naming context", e);
				} catch (NameNotFoundException e) {
					logger.warn("Java naming context doesn't yet contain our stuff (jboss in process of booting?)", e);					
				} catch (NamingException e) {
					throw new RuntimeException(e);
				} catch (JMSException e) {
					logger.warn("JMS exception opening queue", e);
				}
				if (init == null) {
					closeAndWaitRetryInterval();
				} else {
					try {
						init.getConnection().setExceptionListener(new ExceptionListener() {
	
							public void onException(JMSException e) {
								logger.warn("Exception on JMS connection", e);
								
								// We are in some JMS thread here, 
								// and not in our main thread... which 
								// is the only reason we need all this 
								// initLock crack
								close();
							}
							
						});
					} catch (JMSException e) {
						// you have got to be kidding me that setExceptionListener
						// throws a checked exception...
						logger.error("Failed to set exception listener on JMS connection");
					}
					
					//logger.debug("Successful init of JMS queue");
					
					// wake up anyone else waiting for init
					initCondition.signalAll();
				}
			}
		} finally {
			initLock.unlock();
		}
		assert init != null;
		return init;
	}
	
	public void close() {
		initLock.lock();
		try {
			if (init != null) {
				init.close();
			}
		} finally {
			// just forget about anything we couldn't close, what else 
			// are we supposed to do?
			if (init != null) {
				init = null;
				// signal that init state has changed
				initCondition.signalAll();
			}
			
			initLock.unlock();
		}
	}
}
