package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;

public class JmsConsumer extends JmsQueue {
	
	private MessageConsumer messageConsumer;
	
	public JmsConsumer(String queue) {
		super(queue);

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
	
	public void close() {
		try {
			messageConsumer.close();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
		super.close();
	}
}

