package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ChatRoom;
import com.dumbhippo.persistence.ChatRoomMessage;
import com.dumbhippo.persistence.ChatRoomScreenName;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.ChatRoomSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class ChatRoomSystemBean implements ChatRoomSystem {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(ChatRoomSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public ChatRoom lookupChatRoom(String chatRoomName) {
		Query q;
		
		q = em.createQuery("from ChatRoom cr where cr.name = :name");
		q.setParameter("name", chatRoomName);
		
		ChatRoom chatRoom = null;
		try {
			chatRoom = (ChatRoom) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			;
		}
		
		return chatRoom;	
	}
	
	public ChatRoom getChatRoom(Guid postId) throws ValidationException {
		ChatRoomSystem proxy = (ChatRoomSystem) ejbContext.lookup(ChatRoomSystem.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateChatRoom(postId);
			} catch (ValidationException e) {
				throw e;
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating chat room, retrying");
					retries--;
				} else {
					logger.error("Couldn't create ChatRoom", e);					
					throw new RuntimeException("Unexpected error creating chat room", e);
				}
			}
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public ChatRoom findOrCreateChatRoom(Guid postId) {
		Post p = null;
		try {
			p = identitySpider.lookupGuid(Post.class, postId);
		} catch (NotFoundException e) {
			logger.warn("Couldn't find post with ID " + postId + " to find or create chatRoom", e);
			throw new RuntimeException("Couldn't find post with ID " + postId + " to find or create chatRoom", e);
		}
	
		ChatRoom chatRoom = p.getChatRoom();
		if (chatRoom == null) {
			try {
				chatRoom = new ChatRoom(ChatRoom.createChatRoomNameStringFor(p), new Date(), p);
				p.setChatRoom(chatRoom);
				em.persist(chatRoom);
				em.persist(p);
			} catch (Exception e) {
				logger.error("exception saving chat room", e);
				throw new RuntimeException("exception saving chat room", e);
			}
		}
		return chatRoom;
	}
	
	public void addChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp) {
		ChatRoomSystem proxy = (ChatRoomSystem) ejbContext.lookup(ChatRoomSystem.class.getCanonicalName());
	
		
		try {
			proxy.doAddChatRoomMessage(chatRoom, fromScreenName, messageText, timestamp);
		} catch (Exception e) {
			logger.error("caught in addChatRoomMessage ", e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void doAddChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp) {
		
		ChatRoom attachedChatRoom = em.find(ChatRoom.class, chatRoom.getId());
	
		
		logger.debug("chat room messages before update was");
		for (ChatRoomMessage msg: attachedChatRoom.getMessages()) {
			logger.debug("  " + msg.getMessageText());
		}
		
		ChatRoomMessage chatRoomMessage = new ChatRoomMessage(chatRoom, fromScreenName, messageText, timestamp);
		em.persist(chatRoomMessage);
		
		// Update ChatRoom object and persist it
		attachedChatRoom.setLastActivity(chatRoomMessage.getTimestamp());
		attachedChatRoom.getMessages().add(chatRoomMessage);
		
		/* em.persist(attachedChatRoom);  // necessary? */
		
		logger.debug("chat room messages after update was");
		for (ChatRoomMessage msg: attachedChatRoom.getMessages()) {
			logger.debug("  " + msg.getMessageText());
		}
	}

	public void updateChatRoomRoster(ChatRoom chatRoom, List<String> chatRoomRoster) {
		ChatRoomSystem proxy = (ChatRoomSystem) ejbContext.lookup(ChatRoomSystem.class.getCanonicalName());
	
		try {
			proxy.doUpdateChatRoomRoster(chatRoom, chatRoomRoster);
		} catch (Exception e) {
			logger.error("caught in updateChatRoomRoster ", e);
		}
		
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void doUpdateChatRoomRoster(ChatRoom chatRoom, List<String> chatRoomRoster) {
		
		ChatRoom attachedChatRoom = em.find(ChatRoom.class, chatRoom.getId());
		
		logger.debug("chat room roster before update was");
		for (ChatRoomScreenName name: attachedChatRoom.getRoster()) {
			logger.debug("  " + name.getScreenName());
		}
		
		// wipe out the current cached roster and repopulate it based on this request
		attachedChatRoom.getRoster().clear();
		for (String rosterName: chatRoomRoster) {
			ChatRoomScreenName chatRoomScreenName = new ChatRoomScreenName(chatRoom, rosterName);
			em.persist(chatRoomScreenName);
			attachedChatRoom.getRoster().add(chatRoomScreenName);	
		}
		
		attachedChatRoom.setLastActivity(new Date());
		em.persist(attachedChatRoom);
		
		logger.debug("chat room roster after update was");
		for (ChatRoomScreenName name: attachedChatRoom.getRoster()) {
			logger.debug("  " + name.getScreenName());
		}
		
	}
	
}
