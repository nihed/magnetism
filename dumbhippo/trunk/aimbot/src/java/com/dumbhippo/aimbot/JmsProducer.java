package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

public class JmsProducer extends JmsQueue {
	private MessageProducer messageProducer;
	
	public JmsProducer(String queue, boolean local) {
		super(queue, local);
		
		try {
			messageProducer = getSession().createProducer(getDestination());
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	public TextMessage createTextMessage(String text) {
		try {
			return getSession().createTextMessage(text);
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	public void send(Message message) {
		try {
			messageProducer.send(message);
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
	}
	
	@Override
	public void close() {
		try {
			messageProducer.close();
		} catch (JMSException e) {
			throw new JmsUncheckedException(e);
		}
		super.close();
	}
}
