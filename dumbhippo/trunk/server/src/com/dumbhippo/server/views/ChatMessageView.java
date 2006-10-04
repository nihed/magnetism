package com.dumbhippo.server.views;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ChatMessage;

public class ChatMessageView {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ChatMessageView.class);
	private ChatMessage msg;
	private PersonView sender;
	
	public ChatMessageView(ChatMessage msg, PersonView sender) {
		this.msg = msg;
		this.sender = sender;
	}
	
	public ChatMessage getMsg() {
		return msg;
	}

	public PersonView getSenderView() {
		return sender;
	}

	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("message",
				"serial", Long.toString(msg.getId()),
				"timestamp",Long.toString(msg.getTimestamp().getTime()),
				"sender", msg.getFromUser().getId());
		builder.appendTextNode("text", msg.getMessageText());
		builder.closeElement();		
	}
}
