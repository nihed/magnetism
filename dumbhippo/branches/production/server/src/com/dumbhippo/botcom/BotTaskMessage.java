package com.dumbhippo.botcom;

public class BotTaskMessage extends BotTask {

	private static final long serialVersionUID = 1L;

	private String recipient;
	private String htmlMessage;
	
	/**
	 * @param botName name of bot to use, null if not important
	 * @param recipient screen name to send to
	 * @param htmlMessage message html
	 */
	public BotTaskMessage(String botName, String recipient, String htmlMessage) {
		super(botName);
		this.recipient = recipient;
		this.htmlMessage = htmlMessage;
	}
	
	public String getHtmlMessage() {
		return htmlMessage;
	}
	
	public String getRecipient() {
		return recipient;
	}
}
