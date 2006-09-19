package com.dumbhippo.services;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;

public class FacebookSaxHandler extends EnumSaxHandler<FacebookSaxHandler.Element>{
	static private final Logger logger = GlobalSetup.getLogger(FacebookSaxHandler.class);
	
	enum Element {	
		// General
		result,
		
		// facebook.auth.getSession
		session_key,
		uid,
		
		// facebook.messages.getCount
		unread,
		total,
		
		// error message
		fb_error,
		code,
        msg,
            
		IGNORED // an element we don't care about
	}
	
	public enum FacebookErrorCode {
		API_EC_UNKNOWN(1),
		API_EC_SERVICE(2),
		API_EC_TOO_MANY_CALLS(4),
		API_EC_BAD_IP(5),
		API_EC_PARAM_API_KEY(101),
		API_EC_PARAM_SESSION_KEY(102),
		API_EC_PARAM_CALL_ID(103),
		API_EC_PARAM_SIGNATURE(104);
		
		private int code;
		
		FacebookErrorCode(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}			
	}
			
	private String sessionKey;
	private String facebookUserId;
	private int unreadMessageCount;
	private int totalMessageCount;
	private int wallMessageCount;
	private int errorCode;
	private String errorMessage;

	FacebookSaxHandler() {
		super(Element.class, Element.IGNORED);
		unreadMessageCount = -1;
		totalMessageCount = -1;
		wallMessageCount = -1;
		errorCode = -1;
	}

	@Override
	protected void closeElement(Element c) throws SAXException {	
		String currentContent = getCurrentContent().trim();
		try {
			if (c == Element.session_key) {
				sessionKey = currentContent;
				logger.debug("Parsed out sessionKey {}", sessionKey);
			} else if (c == Element.uid) {
				facebookUserId = currentContent;
				logger.debug("Parsed out uid {}", facebookUserId);
			} else if (c == Element.unread) {
				unreadMessageCount = Integer.parseInt(currentContent);
				logger.debug("Parsed out unread message count {}", unreadMessageCount);		
			} else if (c == Element.total) {
				totalMessageCount = Integer.parseInt(currentContent);
				logger.debug("Parsed out total message count {}", totalMessageCount);
			} else if (c == Element.result) {
				// we will parse our an error message before we will be closing
				// the result element; we should not try to parse out regular contents
				// if we got back an error message
				if (errorMessage != null) {
					return;
				}
				Attributes attrs = currentAttributes();
				if (attrs.getValue("method").equals("facebook.wall.getCount")) {
					wallMessageCount = Integer.parseInt(currentContent);
					logger.debug("Parsed out wall message count {}", wallMessageCount);
				}
			} else if (c == Element.code) {
				errorCode = Integer.parseInt(currentContent);	
				logger.debug("Parsed out error code {}", errorCode);
			} else if (c == Element.msg) {
				errorMessage = currentContent; 
				logger.debug("Parsed out error message {}", errorMessage);
			}	
		} catch (NumberFormatException e) {
			logger.warn("Facebook web services content {} for element {} was not a valid number",
					    currentContent, c);
		}
	}
	
	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getFacebookUserId() {
		return facebookUserId;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public static Logger getLogger() {
		return logger;
	}

	public int getTotalMessageCount() {
		return totalMessageCount;
	}

	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}

	public int getWallMessageCount() {
		return wallMessageCount;
	}
}
