package com.dumbhippo.server.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.MetaConstruct;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Sentiment;

@DMO(classId="http://mugshot.org/p/o/chatMessage", resourceBase="/o/chatMessage")
@DMFilter("viewer.canSeeChatMessage(this)")
public abstract class ChatMessageDMO extends DMObject<ChatMessageKey> {
	protected ChatMessage message;
	
	@Inject
	protected DMSession session;
	
	@Inject
	protected EntityManager em;

	protected ChatMessageDMO(ChatMessageKey key) {
		super(key);
	}

	@MetaConstruct
	public static Class<? extends ChatMessageDMO> getDMOClass(ChatMessageKey key) {
		switch (key.getType()) {
		case BLOCK:
			return BlockMessageDMO.class;
		case POST:
			return PostMessageDMO.class;
		case GROUP:
		case TRACK:
			return null;
		}
		
		return null;
	}

	@DMProperty(defaultInclude=true)
	public UserDMO getSender() {
		return session.findUnchecked(UserDMO.class, message.getFromUser().getGuid());
	}
	
	@DMProperty(defaultInclude=true)
	public String getText(){
		return message.getMessageText();
	}
	
	@DMProperty(defaultInclude=true)
	public long getTime() {
		return message.getTimestamp().getTime();
	}
	
	@DMProperty(defaultInclude=true)
	public String getSentiment() {
		if (message.getSentiment() == Sentiment.INDIFFERENT)
			return null;
		else
			return message.getSentiment().name();
	}
	
	@DMProperty 
	public abstract StoreKey<?,?> getVisibilityDelegate();
}
