package com.dumbhippo.server.views;

import org.slf4j.Logger;

import com.dumbhippo.DateUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Sentiment;

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
	
	public String getTimeAgo() {
		return DateUtils.formatTimeAgo(msg.getTimestamp());
	}
	
	private String getSentiment() {
		// Small amount of protocol compression, default to INDIFFERENT
		if (msg.getSentiment() == Sentiment.INDIFFERENT)
			return null;
		else
			return msg.getSentiment().name();
	}

	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.openElement("message",
				"serial", Long.toString(msg.getId()),
				"sentiment", getSentiment(),
				"timestamp",Long.toString(msg.getTimestamp().getTime()),
				"sender", msg.getFromUser().getId());
		builder.appendTextNode("text", msg.getMessageText());
		builder.closeElement();		
	}
}
