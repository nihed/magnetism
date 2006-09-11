package com.dumbhippo.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Version of JmsSessionFactory used in the non-transactional case, where
 * there is one underlying session object for each JmsSession. 
 *  
 * @author otaylor
 */
class NonTxJmsSessionFactory implements JmsSessionFactory {
	public Connection connection;
	
	public NonTxJmsSessionFactory(Context namingContext) throws NamingException, JMSException {
		ConnectionFactory factory = (ConnectionFactory)namingContext.lookup("ConnectionFactory");
		connection = factory.createConnection();
	}
	
	public JmsSession createSession() throws JMSException {
		return new NonTxJmsSession(connection.createSession(false, Session.AUTO_ACKNOWLEDGE));
	}
	
	public void close() throws JMSException {
		connection.close();
	}
}
