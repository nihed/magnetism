package com.dumbhippo.server;

import javax.ejb.ApplicationException;

import com.dumbhippo.XmlBuilder;

@ApplicationException
public class HumanVisibleException extends Exception {
	private static final long serialVersionUID = 0L;

	private String htmlMessage;
	private String suggestionHtml;
	
	public HumanVisibleException(String message) {
		this(message, false);
	}
	
	public HumanVisibleException(String message, boolean isHtml) {
		super(message);
		if (isHtml)
			htmlMessage = message;
	}
	
	public String getHtmlMessage() {
		if (htmlMessage == null)
			return XmlBuilder.escape(getMessage());
		else
			return htmlMessage;
	}
	
	
	/**
	 * This should be a link suggesting where to go next.
	 * @param suggestionHtml the link html
	 * @return this object, for chained calls idiom
	 */
	public HumanVisibleException setHtmlSuggestion(String suggestionHtml) {
		this.suggestionHtml = suggestionHtml;
		return this;
	}
	
	public String getHtmlSuggestion() {
		return suggestionHtml;
	}
}
