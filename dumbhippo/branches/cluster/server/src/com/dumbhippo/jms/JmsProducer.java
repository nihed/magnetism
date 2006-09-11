package com.dumbhippo.jms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * Convenience API for sending messages to a JMS Queue or topic. It manages connections
 * and sessions internally (sessions are per-message without transactions and 
 * per-transactions with transactions) and automatically takes care of reconnecting
 * and retrying when an exception is received sending a message.
 * 
 * @author hp
 * @author otaylor
 */
public class JmsProducer extends JmsDestination {
	private static final Logger logger = GlobalSetup.getLogger(JmsProducer.class);

	public JmsProducer(String destinationName, JmsConnectionType connectionType) {
		super(destinationName, connectionType);
	}
	
	public void sendObjectMessage(Serializable payload) {
		JMSException lastException = null;
		
		for (int i = 0; i < MAX_RETRIES; i++) {
			try {
				JmsSession session = createSession();
				Message message = session.createObjectMessage(payload);
				session.getProducer(getDestination()).send(message);
				session.close();
				
				return;
			} catch (JMSException e) {
				lastException = e;
			}
			
			logger.warn("Got exception trying to send ObjectMessage", lastException);
			closeOnFailure(); // Will close session as a side-effect
		}
		
		logger.error("Hit max retry count sending ObjectMessage, giving up");
		setRollbackOnly();
		throw new JmsUncheckedException(lastException); 
	}
}
