package com.dumbhippo.server.impl;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventLogin;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.ResourceClaimToken;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AimQueueSender;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.TokenExpiredException;
import com.dumbhippo.server.TokenSystem;
import com.dumbhippo.server.TokenUnknownException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.tx.RetryException;

@MessageDriven(activationConfig =
 {
   @ActivationConfigProperty(propertyName="destinationType",
     propertyValue="javax.jms.Queue"),
   @ActivationConfigProperty(propertyName="destination",
     propertyValue=BotEvent.QUEUE_NAME)
})
public class AimQueueConsumerBean implements MessageListener {
	static private final Logger logger = GlobalSetup.getLogger(AimQueueConsumerBean.class);
	
	@EJB
	private TokenSystem tokenSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private SigninSystem signinSystem;
	
	@EJB
	private AimQueueSender aimQueueSender;
	
	private void sendHtmlReplyMessage(BotEvent event, String aimName, String htmlMessage) {
		BotTaskMessage message = new BotTaskMessage(event.getBotName(), aimName, htmlMessage);
		aimQueueSender.sendMessage(message);
	}
	
	private void sendReplyMessage(BotEvent event, String aimName, String textMessage) {
		sendHtmlReplyMessage(event, aimName, XmlBuilder.escape(textMessage));
	}
	
	private void processTokenEvent(final BotEventToken event) throws RetryException {
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
			logger.warn("Event token was of the wrong type, expecting ResourceClaimToken, got {}", 
					token.getClass().getName());
			sendReplyMessage(event, event.getAimName(), "It looks like your code has expired!");
			return;
		}
		
		final ResourceClaimToken claim = (ResourceClaimToken) token;
		
		final AimResource resource;
		try {
			resource = identitySpider.getAim(event.getAimName());
		} catch (ValidationException e) {
			logger.error("Got invalid screen name from AIM: probably should not have been considered invalid: '{}': {}", event.getAimName(), e.getMessage());
			throw new RuntimeException("broken, invalid screen name from AIM bot", e);
		}
		
		DataService.currentSessionRW().runAsSystem(new Runnable() {
			public void run() {
				try {
					claimVerifier.verify(SystemViewpoint.getInstance(), claim, resource);
					sendReplyMessage(event, event.getAimName(), "The screen name " + event.getAimName() + " was added to your Mugshot account");
				} catch (HumanVisibleException e) {
					logger.debug("exception verifying claim, sending back to user: {}", e.getMessage());
					sendHtmlReplyMessage(event, event.getAimName(), e.getHtmlMessage());
				}
			}
		});	
	}
	
	private void processLoginEvent(BotEventLogin event) throws RetryException {
		try {
			String htmlSigninLinkMessage = signinSystem.getSigninLinkAim(event.getAimName());
			sendHtmlReplyMessage(event, event.getAimName(), htmlSigninLinkMessage);
		} catch (HumanVisibleException e) {
			logger.warn("exception getting signin link, sending back to user: {} ", e.getMessage());
			sendHtmlReplyMessage(event, event.getAimName(), e.getHtmlMessage());
		}
	}

	public void onMessage(Message message) {
		DataService.getModel().initializeReadWriteSession(AnonymousViewpoint.getInstance(Site.NONE));
		
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in {}: {}", BotEvent.QUEUE_NAME, obj);
					
				if (obj instanceof BotEventToken) {
					BotEventToken event = (BotEventToken) obj;
					processTokenEvent(event);
				} else if (obj instanceof BotEventLogin) {
					BotEventLogin event = (BotEventLogin) obj;
					processLoginEvent(event);
				} else {
					logger.warn("Got unknown object: " + obj);
				}
			} else {
				logger.warn("Got unknown jms message: {}", message);
			}
		} catch (RetryException e) {
			// We hope that the messaging system will redeliver the message and provide
			// the retry. We can't do the retry ourself since we are already in a 
			// transaction
			throw new RuntimeException(e);
		} catch (JMSException e) {
			logger.warn("JMS exception in bot event queue", e);
		}
	}
}
