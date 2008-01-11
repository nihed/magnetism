package com.dumbhippo.jive;

import java.util.Date;

import javax.ejb.EJB;

import org.xmpp.packet.IQ;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

@IQHandler(namespace=ChatMessagesIQHandler.CHAT_MESSAGES_NAMESPACE)
public class ChatMessagesIQHandler extends AnnotatedIQHandler {
	static final String CHAT_MESSAGES_NAMESPACE = "http://mugshot.org/p/chatMessages"; 
	
	@EJB
	private ChatSystem chatSystem;
	
	public ChatMessagesIQHandler() {
		super("Mugshot Chat Messages IQ Handler");
	}
	
	@IQMethod(name="addMessage", type=IQ.Type.set)
	@IQParams({ "chatId", "text", "sentiment" })
	public void addChatMessage(UserViewpoint viewpoint, Guid chatId, String text, String sentiment) throws IQException, RetryException {
		Sentiment sentimentValue;
		try {
			sentimentValue = Sentiment.valueOf(sentiment);
		} catch (IllegalArgumentException e) {
			throw IQException.createBadRequest("Bad value for sentiment");
		}
		
		ChatRoomInfo info;
		try {
			info = chatSystem.getChatRoomInfo(chatId, false);
			if (!chatSystem.canJoinChat(info.getChatId(), info.getKind(), viewpoint))
				throw new NotFoundException("Chatroom not visible");
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("No such chatroom");
		}
		
		chatSystem.addChatRoomMessage(info.getChatId(), info.getKind(), viewpoint, text, sentimentValue, new Date());
	}
}
