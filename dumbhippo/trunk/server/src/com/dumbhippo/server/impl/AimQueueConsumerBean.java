package com.dumbhippo.server.impl;

import java.util.Map;

import javax.annotation.EJB;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotEventUserPresence;
import com.dumbhippo.botcom.BotTask;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;

@MessageDriven(activateConfig =
 {
   @ActivationConfigProperty(propertyName="destinationType",
     propertyValue="javax.jms.Queue"),
   @ActivationConfigProperty(propertyName="destination",
     propertyValue="queue/" + BotEvent.QUEUE)
})
public class AimQueueConsumerBean implements MessageListener {
	static private final Logger logger = GlobalSetup.getLogger(AimQueueConsumerBean.class);
	
	@EJB
	private TokenSystem tokenSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private IdentitySpider identitySpider;
	
	private void sendHtmlReplyMessage(BotEvent event, String aimName, String htmlMessage) {
		BotTaskMessage message = new BotTaskMessage(event.getBotName(), aimName, htmlMessage);
		JmsProducer producer = new JmsProducer(BotTask.QUEUE, true);
		ObjectMessage jmsMessage = producer.createObjectMessage(message);
		logger.debug("Sending JMS message to " + BotTask.QUEUE + ": " + jmsMessage);
		producer.send(jmsMessage);
	}
	
	private void sendReplyMessage(BotEvent event, String aimName, String textMessage) {
		sendHtmlReplyMessage(event, aimName, XmlBuilder.escape(textMessage));
	}
	
	private void processTokenEvent(BotEventToken event) {
		Token token;
		try {
			token = tokenSystem.getTokenByKey(event.getToken());
		} catch (TokenExpiredException e) {
			sendReplyMessage(event, event.getAimName(), "It looks like your code has expired!");
			return;
		} catch (TokenUnknownException e) {
			sendReplyMessage(event, event.getAimName(), "Hmm, the code you gave me doesn't look right. Try again?");
			return;
		} 
		if (!(token instanceof ResourceClaimToken)) {
			logger.debug("Event token was of the wrong type");
			sendReplyMessage(event, event.getAimName(), "It looks like your code has expired!");
			return;
		}
		
		ResourceClaimToken claim = (ResourceClaimToken) token;
		
		AimResource resource;
		try {
			resource = identitySpider.getAim(event.getAimName());
		} catch (ValidationException e) {
			logger.error("Got invalid screen name from AIM: probably should not have been considered invalid: '" + event.getAimName() + "'", e);
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
	
	/*
	 * Handle an event from the bot specifying the online/offline status of
	 * one or more screen names.
	 * 
	 * @param event
	 */
	private void processUserPresence(BotEventUserPresence event) {
		
		logger.debug("processing user presence event " + event.toString());
		
		Map<String,Boolean> userOnlineMap = event.getUserOnlineMap();
		
		for (String screenName: userOnlineMap.keySet()) {
			Boolean isOnline = (Boolean)userOnlineMap.get(screenName);
			
			logger.debug("processing user presence event part for '" + screenName + "' of " + (isOnline.booleanValue() ? "online" : "offline"));
			
			AimResource aimResource = identitySpider.lookupAim(screenName);
			if (aimResource == null) {
				logger.debug("no AimResource found for screen name '" + screenName + "'");
			} else {
				logger.debug("found an aimResource, looking up matching user for " + aimResource.getId());
				
				User user = identitySpider.getUser(aimResource);
				
				if (user == null) {
					logger.debug("didn't find a matching user for " + aimResource.getId());
					return;
				}
				
				logger.debug("matching user for screen name '" + screenName + "' is " + user.getNickname() + "/" + user.getGuid().toString());
			}
		}
	}
	
	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + BotEvent.QUEUE + ": " + message);
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in " + BotEvent.QUEUE + ": " + obj);
				
				if (obj instanceof BotEventToken) {
					BotEventToken event = (BotEventToken) obj;
					processTokenEvent(event);
				} else if (obj instanceof BotEventUserPresence) {
					BotEventUserPresence event = (BotEventUserPresence) obj;
					processUserPresence(event);
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
