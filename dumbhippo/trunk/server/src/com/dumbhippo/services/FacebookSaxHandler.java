package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookPhotoData;

public class FacebookSaxHandler extends EnumSaxHandler<FacebookSaxHandler.Element>{
	static private final Logger logger = GlobalSetup.getLogger(FacebookSaxHandler.class);
	
	enum Element {	
		// General
		result,
		result_elt,
		total,
		
		// facebook.auth.getSession
		session_key,
		uid,
		
		// facebook.messages.getCount
		unread,
		
		// facebook.pokes.getCount
		unseen,
		
		// facebook.photos.getOfUser
		link,
		src, 
		caption,
		created,
		aid,
		
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
			
	private FacebookAccount facebookAccount;
	private String sessionKey;
	private String facebookUserId;
	private int unreadMessageCount;
	private int totalCount;
	private int totalMessageCount;
	private int wallMessageCount;
	private int unseenPokeCount;
	private int totalPokeCount;
	private int errorCode;
	private String errorMessage;
	private List<FacebookPhotoData> taggedPhotos;
	private boolean gettingTaggedPhotos;

	FacebookSaxHandler() {
		this(null);	
	}

	FacebookSaxHandler(FacebookAccount facebookAccount) {
		super(Element.class, Element.IGNORED);
		this.facebookAccount = facebookAccount;
		unreadMessageCount = -1;
		totalCount = -1;
		totalMessageCount = -1;
		wallMessageCount = -1;
		unseenPokeCount = -1;
		totalPokeCount = -1;
		errorCode = -1;
		taggedPhotos = new ArrayList<FacebookPhotoData>();
		gettingTaggedPhotos = false;
	}
	
	private FacebookPhotoData currentFacebookPhotoData() {
		if (taggedPhotos.size() > 0)
			return taggedPhotos.get(taggedPhotos.size() - 1);
		else
			return null;
	}
	
	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.result) {
			Attributes attrs = currentAttributes();
			if (attrs.getValue("method").equals("facebook.photos.getOfUser")) {
				gettingTaggedPhotos = true; 
			}
		} else if ((c == Element.result_elt) && gettingTaggedPhotos) {
			FacebookPhotoData photo = new FacebookPhotoData();
			photo.setFacebookAccount(facebookAccount);
			taggedPhotos.add(photo);
		}
	}
	
	private int parseFacebookCount(Element c, String content) {
	    try {	
			if (content.equals("")) {
				return 0;
			} else {
			    return Integer.parseInt(content);
			}			
		} catch (NumberFormatException e) {
			logger.warn("Facebook web services content {} for element {} was not a valid number",
					    content, c);
			return -1;
		}
	}

	private long parseFacebookDate(Element c, String content) {
	    try {	
			if (content.equals("")) {
				return -1;
			} else {
				// return the time in milliseconds
			    return Long.parseLong(content) * 1000;
			}			
		} catch (NumberFormatException e) {
			logger.warn("Facebook web services content {} for element {} was not a valid number",
					    content, c);
			return -1;
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {	
		String currentContent = getCurrentContent().trim();
		if (c == Element.session_key) {
			sessionKey = currentContent;
			logger.debug("Parsed out sessionKey {}", sessionKey);
		} else if (c == Element.uid) {
			facebookUserId = currentContent;
			logger.debug("Parsed out uid {}", facebookUserId);
		} else if (c == Element.unread) {
            unreadMessageCount = parseFacebookCount(c, currentContent); 
			logger.debug("Parsed out unread message count {}", unreadMessageCount);	
		} else if (c == Element.unseen) {
			unseenPokeCount = parseFacebookCount(c, currentContent); 
			logger.debug("Parsed out unseen poke count {}", unseenPokeCount);		
		} else if (c == Element.total) {
			totalCount = parseFacebookCount(c, currentContent); 
			logger.debug("Parsed out total count {}", totalCount);
		} else if (c == Element.link) {
			currentFacebookPhotoData().setLink(currentContent);
			logger.debug("Parsed out photo link {}", currentContent);
		} else if (c == Element.src) {
			currentFacebookPhotoData().setSource(currentContent);	
			logger.debug("Parsed out photo source {}", currentContent);
		} else if (c == Element.caption) {
			currentFacebookPhotoData().setCaption(currentContent);
			logger.debug("Parsed out photo caption {}", currentContent);
		} else if (c == Element.created) {
			currentFacebookPhotoData().setCreatedTimestampAsLong(parseFacebookDate(c, currentContent));
			logger.debug("Parsed out photo date {}", currentFacebookPhotoData().getCreatedTimestampAsLong());
		} else if (c == Element.aid) {
			currentFacebookPhotoData().setAlbumId(currentContent);
			logger.debug("Parsed out album id {}", currentContent);
		} else if (c == Element.result) {
			// we will parse our an error message before we will be closing
			// the result element; we should not try to parse out regular contents
			// if we got back an error message
			if (errorMessage != null) {
				return;
			}
			Attributes attrs = currentAttributes();
			if (attrs.getValue("method").equals("facebook.wall.getCount")) {
				wallMessageCount = parseFacebookCount(c, currentContent); 
				logger.debug("Parsed out wall message count {}", wallMessageCount);
			} else if (attrs.getValue("method").equals("facebook.messages.getCount")) {
				totalMessageCount = totalCount;
			} else if (attrs.getValue("method").equals("facebook.pokes.getCount")) {
				totalPokeCount = totalCount;
			}  
		} else if (c == Element.code) {
			errorCode = parseFacebookCount(c, currentContent);
			logger.debug("Parsed out error code {}", errorCode);
		} else if (c == Element.msg) {
			errorMessage = currentContent; 
			logger.debug("Parsed out error message {}", errorMessage);
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

	public int getTotalPokeCount() {
		return totalPokeCount;
	}

	public int getUnseenPokeCount() {
		return unseenPokeCount;
	}
	
	public int getTaggedPhotoCount() {
		return taggedPhotos.size();
	}
	
	public List<FacebookPhotoData> getTaggedPhotos() {
		return taggedPhotos;
	}
}
