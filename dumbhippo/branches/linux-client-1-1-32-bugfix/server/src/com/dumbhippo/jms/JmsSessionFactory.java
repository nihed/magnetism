package com.dumbhippo.jms;

import javax.jms.JMSException;

/**
 * An interface for creating JmsSession objects. We implement it in both the transactional 
 * and non-transactional cases.
 */
interface JmsSessionFactory {
	/**
	 * Create a new JmsSession object
	 * 
	 * @return a newly created JmsSession object. Note that while it's always
	 *   a new JmsSession object, the underlying JMS Session object might be
	 *   an existing one, if we are transactional and one already exists for the
	 *   thread.
	 * @throws JMSException
	 */
	public JmsSession createSession() throws JMSException;
	
	/**
	 * Closes the session factory, all sessions created from it, and the 
	 * underlying Connection object. 
	 * @throws JMSException
	 */
	public void close() throws JMSException;
}
