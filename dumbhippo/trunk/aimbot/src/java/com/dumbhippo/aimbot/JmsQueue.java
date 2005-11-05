package com.dumbhippo.aimbot;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JmsQueue {

	private Connection connection;
	private Session session;
	private Destination destination;
	
	protected JmsQueue(String queue) {
		try {
			InitialContext ctx = new InitialContext();
		
			ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup("RMIConnectionFactory");
	
			connection = connectionFactory.createConnection();
		
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
