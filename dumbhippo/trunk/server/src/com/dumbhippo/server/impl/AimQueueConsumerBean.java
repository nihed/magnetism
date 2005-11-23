package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TokenSystem;

@MessageDriven(activateConfig =
 {
   @ActivationConfigProperty(propertyName="destinationType",
     propertyValue="javax.jms.Queue"),
   @ActivationConfigProperty(propertyName="destination",
     propertyValue="queue/" + AimQueueConsumerBean.INCOMING_QUEUE)
})
public class AimQueueConsumerBean implements MessageListener {
	static private final Log logger = GlobalSetup.getLog(AimQueueConsumerBean.class);
	static public final String INCOMING_QUEUE = "IncomingAimQueue";
	static public final String OUTGOING_QUEUE = "OutgoingAimQueue";
	
	@EJB
	private TokenSystem tokenSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private IdentitySpider identitySpider;
	
	private void sendHtmlReplyMessage(BotEvent event, String aimName, String htmlMessage) {
		BotTaskMessage message = new BotTaskMessage(event.getBotName(), aimName, htmlMessage);
		JmsProducer producer = new JmsProducer(OUTGOING_QUEUE, true);
		ObjectMessage jmsMessage = producer.createObjectMessage(message);
		logger.debug("Sending JMS message to " + OUTGOING_QUEUE + ": " + jmsMessage);
		producer.send(jmsMessage);
	}
	
	private void sendReplyMessage(BotEvent event, String aimName, String textMessage) {
		sendHtmlReplyMessage(event, aimName, XmlBuilder.escape(textMessage));
	}
	
	private void processTokenEvent(BotEventToken event) {
		Token token = tokenSystem.lookupTokenByKey(event.getToken()); 
		if (token == null || !(token instanceof ResourceClaimToken)) {
			logger.debug("Event token was expired or bogus");
			sendReplyMessage(event, event.getAimName(), "It looks like your code has expired!");
		} else {
			ResourceClaimToken claim = (ResourceClaimToken) token;
		
			AimResource resource;
			try {
				resource = identitySpider.getAim(event.getAimName());
			} catch (ValidationException e) {
				logger.trace(e);
				logger.error("Got invalid screen name from AIM: probably should not have been considered invalid: '" + event.getAimName() + "'");
				throw new RuntimeException("broken, invalid screen name from AIM bot", e);
			}
			
			try {
				claimVerifier.verify(null, claim, resource);
				sendReplyMessage(event, event.getAimName(), "The screen name " + event.getAimName() + " was added to your account");
			} catch (HumanVisibleException e) {
				logger.debug("exception verifying claim", e);
				sendHtmlReplyMessage(event, event.getAimName(), e.getHtmlMessage());
			}
		}
	}
	
	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + INCOMING_QUEUE + ": " + message);
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in " + INCOMING_QUEUE + ": " + obj);
				
				if (obj instanceof BotEventToken) {
					BotEventToken event = (BotEventToken) obj;
					
					processTokenEvent(event);
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
}
