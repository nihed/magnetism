package com.dumbhippo.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;


/**
 * 
 * There is no reason to use this inside the app server, just 
 * create a message-driven bean. Outside the app server it lets you 
 * read messages from a JMS queue without fighting all the JMS 
 * boilerplate.
 * 
 * @author hp
 *
 */
public class JmsConsumer extends JmsQueue {
	
	private MessageConsumer messageConsumer;
	
	public JmsConsumer(String queue) {
		super(queue, false);

		try {
			messageConsumer = getSession().createConsumer(getDestination());
			getConnection().start();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	public Message receive() {
		try {
			return messageConsumer.receive();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	public Message receive(long timeout) {
		try {
			return messageConsumer.receive(timeout);
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	@Override
	public void close() {
		try {
			messageConsumer.close();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
		super.close();
	}
}

