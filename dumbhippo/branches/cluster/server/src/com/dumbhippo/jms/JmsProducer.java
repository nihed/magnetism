package com.dumbhippo.jms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.naming.NamingException;

/**
 * Message producer convenience object that wraps JMS.
 * Much of the convenience is in converting all the exceptions to unchecked,
 * since the JMS API is fucked and just has one kind of exception blending things
 * that should be checked and things that should not. Also makes 
 * communication failures with JMS block/retry instead of throwing an 
 * exception.
 * 
 * @author hp
 *
 */
public class JmsProducer extends JmsQueue {
	public JmsProducer(String queue, boolean inServer) {
		super(queue, inServer);
	}
	
	public MessageProducer getProducer() {
		return ((ProducerInit)open()).getProducer();
	}
	
	public TextMessage createTextMessage(String text) {
		while (true) {
			try {
				return getSession().createTextMessage(text);
			} catch (JMSException e) {
				close();
			}
		}
	}
	
	public ObjectMessage createObjectMessage(Serializable payload) {
		while (true) {
			try {
				return getSession().createObjectMessage(payload);
			} catch (JMSException e) {
				logger.warn("Error creating object message", e);
				close();
			}
		}
	}
	
	public void send(Message message) {
		while (true) {
			try {
				getProducer().send(message);
				logger.debug("Sent JMS message");
				break;
			} catch (JMSException e) {
				logger.warn("Error sending message", e);
				close();
			}
		}
	}
	
	private static class ProducerInit extends Init {
		private MessageProducer messageProducer;

		ProducerInit(String queue, boolean inServer) throws JMSException, NamingException {
			super(queue, inServer);
		}
		
		@Override
		protected void openSub() throws NamingException, JMSException {
			messageProducer = getSession().createProducer(getDestination());		
		}
		
		@Override
		protected void closeSub() throws JMSException {
			try {
				if (messageProducer != null) {
					messageProducer.close();
				}
			} finally {
				messageProducer = null;
			}
		}
		
		public MessageProducer getProducer() {
			return messageProducer;
		}
	}

	@Override
	protected Init newInit(String queue, boolean inServer) throws JMSException, NamingException {
		return new ProducerInit(queue, inServer);
	}
}
