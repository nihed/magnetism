package com.dumbhippo.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.naming.NamingException;


/**
 * 
 * There is no reason to use this inside the app server, just 
 * create a message-driven bean. Outside the app server it lets you 
 * read messages from a JMS queue without fighting all the JMS 
 * boilerplate. Also makes communication failures block/retry instead
 * of throwing an exception.
 * 
 * @author hp
 *
 */
public class JmsConsumer extends JmsQueue {
	
	public JmsConsumer(String queue, boolean inServer) {
		super(queue, inServer);
	}
	
	public MessageConsumer getConsumer() {
		return ((ConsumerInit)open()).getConsumer();
	}
	
	public Message receive() {
		while (true) {
			try {
				return getConsumer().receive();
			} catch (JMSException e) {
				logger.warn("Exception in receive()", e);
				// There are probably exceptions we shouldn't do this with,
				// but JMS kindly uses the same exception for everything
				close();
			}
		}
	}
	
	public Message receive(long timeout) {
		try {
			return getConsumer().receive(timeout);
		} catch (JMSException e) {
			logger.warn("Exception in receive()", e);
			return null;
		}
	}
	
	private static class ConsumerInit extends Init {
		private MessageConsumer messageConsumer;
		
		ConsumerInit(String queue, boolean inServer) throws JMSException, NamingException {
			super(queue, inServer);
		}

		@Override
		protected void openSub() throws NamingException, JMSException {
			messageConsumer = getSession().createConsumer(getDestination());
			
			// is it OK to do this more than once? of course right now 
			// we always close consumer and connection at the same time
			getConnection().start();				
		}
		
		@Override
		protected void closeSub() throws JMSException {
			try {
				if (messageConsumer != null) {
					messageConsumer.close();
				}
			} finally {
				messageConsumer = null;
			}
		}

		public MessageConsumer getConsumer() {
			return messageConsumer;
		}
	}

	@Override
	protected Init newInit(String queue, boolean inServer) throws JMSException, NamingException {
		return new ConsumerInit(queue, inServer);
	}
}

