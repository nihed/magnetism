package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ChatRoom;
import com.dumbhippo.persistence.ChatRoomMessage;
import com.dumbhippo.persistence.ChatRoomScreenName;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.ChatRoomSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TransactionRunner;

@Stateless
public class ChatRoomSystemBean implements ChatRoomSystem {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ChatRoomSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private TransactionRunner runner;
	
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
	
	public ChatRoom getChatRoom(final Guid postId) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<ChatRoom>() {

				public ChatRoom call() {
					Post p;
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
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}
	
	public void addChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp) {		
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
