package com.dumbhippo.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Utility base class that creates a JMS connection, session, and destination.
 * Half the purpose is to hide all the stuupid JMSException that should not be
 * checked exceptions. 
 * 
 * @author hp
 *
 */
public abstract class JmsQueue {

	private Connection connection;
	private Session session;
	private Destination destination;
	
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
			
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	protected Destination getDestination() {
		return destination;
	}
	
	protected Session getSession() {
		return session;
	}
	
	protected Connection getConnection() {
		return connection;
	}
	
	public void close() {
		try {
			session.close();
			connection.close();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
}
