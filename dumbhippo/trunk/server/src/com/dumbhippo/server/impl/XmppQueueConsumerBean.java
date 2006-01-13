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
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventChatMessage;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;

@MessageDriven(activateConfig =
{
  @ActivationConfigProperty(propertyName="destinationType",
    propertyValue="javax.jms.Queue"),
  @ActivationConfigProperty(propertyName="destination",
    propertyValue="queue/" + XmppEvent.QUEUE)
})
public class XmppQueueConsumerBean implements MessageListener {

	static private final Log logger = GlobalSetup.getLog(XmppQueueConsumerBean.class);

	@EJB
	MusicSystem musicSystem;
	
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
		User user = null;
		try {
			user = identitySpider.lookupGuid(User.class, Guid.parseJabberId(username));
		} catch (NotFoundException e) {
		} catch (Guid.ParseException e) {
		}
		
		return user;
	}
	
	private Post getPostFromRoomName(User user, String roomName) {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Post post = null;
		try {
			post = postingBoard.loadRawPost(viewpoint, Guid.parseJabberId(roomName));
		} catch (Guid.ParseException e) {
		}
		
		return post;
	}
	
	
	public void processChatMessageEvent(XmppEventChatMessage event) {
		User fromUser = getUserFromUsername(event.getFromUsername());
		if (fromUser == null)
			throw new RuntimeException("non-existant username: " + event.getFromUsername());
				
		Post post = getPostFromRoomName(fromUser, event.getRoomName());
		if (post == null)
			return;
		
		postingBoard.addPostMessage(post, fromUser, event.getText(), event.getTimestamp());
	}	
}
