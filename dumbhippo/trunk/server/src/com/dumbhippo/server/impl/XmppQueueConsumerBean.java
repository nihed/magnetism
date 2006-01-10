package com.dumbhippo.server.impl;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;

@MessageDriven(activateConfig =
{
  @ActivationConfigProperty(propertyName="destinationType",
    propertyValue="javax.jms.Queue"),
  @ActivationConfigProperty(propertyName="destination",
    propertyValue="queue/" + XmppQueueConsumerBean.INCOMING_QUEUE)
})
public class XmppQueueConsumerBean implements MessageListener {

	static private final Log logger = GlobalSetup.getLog(XmppQueueConsumerBean.class);
	static public final String INCOMING_QUEUE = "IncomingXMPPQueue";
	static public final String OUTGOING_QUEUE = "OutgoingXMPPQueue";
	
	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + INCOMING_QUEUE + ": " + message);
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in " + INCOMING_QUEUE + ": " + obj);
				
				if (obj instanceof XmppEventMusicChanged) {
					XmppEventMusicChanged event = (XmppEventMusicChanged) obj;
					processMusicChangedEvent(event);
				} else {
					logger.warn("Got unknown object: " + obj);
				}
			} else {
				logger.warn("Got unknown jms message: " + message);
			}
		} catch (JMSException e) {
			logger.warn("JMS exception", e);
		}
	}


	private void processMusicChangedEvent(XmppEventMusicChanged event) {
		logger.debug("FIXME");
	}
}
