package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventChatMessage;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;
import com.dumbhippo.xmppcom.XmppEventRoomPresenceChange;

@MessageDriven(activateConfig =
{
  @ActivationConfigProperty(propertyName="destinationType",
    propertyValue="javax.jms.Queue"),
  @ActivationConfigProperty(propertyName="destination",
    propertyValue="queue/" + XmppEvent.QUEUE)
})
public class XmppQueueConsumerBean implements MessageListener {

	static private final Logger logger = GlobalSetup.getLogger(XmppQueueConsumerBean.class);

	@EJB
	private MusicSystemInternal musicSystem;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	IdentitySpider identitySpider;
	
	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + XmppEvent.QUEUE + ": " + message);
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in " + XmppEvent.QUEUE + ": " + obj);
				
				if (obj instanceof XmppEventMusicChanged) {
					XmppEventMusicChanged event = (XmppEventMusicChanged) obj;
					processMusicChangedEvent(event);
				} else if (obj instanceof XmppEventChatMessage) {
					XmppEventChatMessage event = (XmppEventChatMessage) obj;
					processChatMessageEvent(event);					
				} else if (obj instanceof XmppEventRoomPresenceChange) {
					XmppEventRoomPresenceChange event = (XmppEventRoomPresenceChange) obj;
					processRoomPresenceChangeEvent(event);
				} else {
					logger.warn("Got unknown object: " + obj);
				}
			} else {
				logger.warn("Got unknown jms message: " + message);
			}
		} catch (JMSException e) {
			logger.warn("JMS exception", e);
		} catch (Exception e) {
			logger.warn("Exception processing Xmpp JMS message: " + ExceptionUtils.getRootCause(e).getMessage(), e);
		}
	}

	private void processMusicChangedEvent(XmppEventMusicChanged event) {
		Guid guid;
		try {
			guid = Guid.parseJabberId(event.getJabberId());
		} catch (ParseException e) {
			logger.warn("Invalid jabber ID in music changed event: " + event.getJabberId());
			throw new RuntimeException(e);
		}
		User user;
		try {
			user = identitySpider.lookupGuid(User.class, guid);
		} catch (NotFoundException e) {
			logger.warn("Unknown user in music changed event");
			throw new RuntimeException(e);
		}
		musicSystem.setCurrentTrack(user, event.getProperties());
	}
	
	private User getUserFromUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted username " + username + " did not exist for some reason");
		}
	}
	
	private Post getPostFromRoomName(User user, String roomName) {
		Viewpoint viewpoint = new Viewpoint(user);
		try {
			return postingBoard.loadRawPost(viewpoint, Guid.parseTrustedJabberId(roomName));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted room name did not exist" + roomName, e);
		}
	}
	
	
	public void processChatMessageEvent(XmppEventChatMessage event) {
		User fromUser = getUserFromUsername(event.getFromUsername());
		Post post = getPostFromRoomName(fromUser, event.getRoomName());		
		postingBoard.addPostMessage(post, fromUser, event.getText(), event.getTimestamp(), event.getSerial());
	}
	
	private void processRoomPresenceChangeEvent(XmppEventRoomPresenceChange event) {
		User user = getUserFromUsername(event.getUsername());
		Post post = getPostFromRoomName(user, event.getRoomName());
		LiveState live = LiveState.getInstance();
		live.postPresenceChange(post.getGuid(), user.getGuid(), event.isPresent());
	}
}
