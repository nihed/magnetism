package com.dumbhippo.server.impl;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

@MessageDriven(activateConfig =
 {
   @ActivationConfigProperty(propertyName="destinationType",
     propertyValue="javax.jms.Queue"),
   @ActivationConfigProperty(propertyName="destination",
     propertyValue="queue/" + AimQueueConsumerBean.QUEUE)
})
public class AimQueueConsumerBean implements MessageListener {
	static private final Log logger = GlobalSetup.getLog(AimQueueConsumerBean.class);
	static public final String QUEUE = "IncomingAimQueue";
	
	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + QUEUE + ": " + message);
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				
				logger.debug("Text: " + textMessage.getText());
				
				
			}
		} catch (JMSException e) {
			logger.warn("JMS exception", e);
		}
	}
}
