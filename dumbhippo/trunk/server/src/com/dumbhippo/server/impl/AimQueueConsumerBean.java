package com.dumbhippo.server.impl;

import java.util.List;

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
import com.dumbhippo.botcom.BotEventChatRoomRoster;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ChatRoomStatusCache;
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
	
	private void processChatRoomRosterEvent(BotEventChatRoomRoster event) {
		String chatRoomName = event.getChatRoomName();
		List<String> chatRoomRoster = event.getChatRoomRoster();
		
		logger.debug("processing chat room roster event for '" + chatRoomName + "' with " + chatRoomRoster);
		
		// currently chat room status is cached in a single static object
		ChatRoomStatusCache.putChatRoomStatus(chatRoomName, chatRoomRoster);
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
				} else if (obj instanceof BotEventChatRoomRoster) {
					BotEventChatRoomRoster event = (BotEventChatRoomRoster) obj;
					processChatRoomRosterEvent(event);
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
