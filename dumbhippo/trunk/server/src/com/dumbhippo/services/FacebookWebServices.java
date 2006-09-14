package com.dumbhippo.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

public class FacebookWebServices extends AbstractXmlRequest<FacebookSaxHandler> {
	static private final Logger logger = GlobalSetup.getLogger(FacebookWebServices.class);

	private String apiKey;
	private String secret;
	
	public FacebookWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		try {
			apiKey = config.getPropertyNoDefault(HippoProperty.FACEBOOK_API_KEY).trim();
			if (apiKey.length() == 0)
				apiKey = null;
			this.secret = config.getPropertyNoDefault(HippoProperty.FACEBOOK_SECRET).trim();
			if (secret.length() == 0)
				secret = null;			
		} catch (PropertyNotFoundException e) {
			apiKey = null;
			secret = null;
		}
		if ((apiKey == null) || (secret == null))
			logger.warn("Facebook API key or secret are not set, can't make Facebook web services calls.");
	}
	
	public void updateSession(FacebookAccount facebookAccount, String facebookAuthToken) {
		List<String> params = new ArrayList<String>();
		params.add("method=facebook.auth.getSession");
		params.add("auth_token=" + facebookAuthToken);
		
		String wsUrl = generateFacebookRequest(params);
			
		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(), wsUrl);
		facebookAccount.setSessionKey(handler.getSessionKey());
		facebookAccount.setFacebookUserId(handler.getFacebookUserId());
		if (handler.getSessionKey() != null)
	        facebookAccount.setSessionKeyValid(true);			
	}
	
	/**
	 * Return true if the number of unread messages changed or the number of total messages 
	 * increased and some of them are unread messages.
	 * 
	 * @param facebookAccount
	 * @return true if the set of unread messages changed
	 */
	public boolean updateMessageCount(FacebookAccount facebookAccount) {
	    // generate messages request using session from facebookAccount
		List<String> params = new ArrayList<String>();
        params.add("method=facebook.messages.getCount");
		params.add("session_key=" + facebookAccount.getSessionKey());

		String wsUrl = generateFacebookRequest(params);

		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(), wsUrl);
		boolean newMessagesChanged = false;
		int newUnread = handler.getUnreadMessageCount(); 
		int newTotal = handler.getTotalMessageCount();
		int oldUnread = facebookAccount.getUnreadMessageCount();
		int oldTotal = facebookAccount.getTotalMessageCount();
		if ((newUnread != -1) && (newTotal != -1)) {
			// with the current api, we can only know for sure that the set of new messages
			// changed if newUnread != oldUnread; however there is another situation when 
			// the user could have new messages: if newTotal > oldTotal, the user must have received 
			// new messages, though we do not know if they read the new ones and left the 
			// old unread messages unread or actually read the old unread messages, and then 
			// received the new ones			
            // also, deleted messages are not included in the number of total messages we get 
			// from facebook, which prevents us from always notifying the user if they possibly
			// have new messages
			// so we would not detect that the person has new messages if, for example, between 
			// our requests they read 2 of their new messages, deleted them, and received 2 other 
			// new message 
			if ((newUnread != oldUnread) || ((newTotal > oldTotal) && (newUnread > 0)))  {
				newMessagesChanged = true;							
			}
			// update the number of messages fields on facebookAccount
			facebookAccount.setUnreadMessageCount(newUnread);
			facebookAccount.setTotalMessageCount(newTotal);
		} else if (handler.getErrorCode() == FacebookSaxHandler.FacebookErrorCode.API_EC_PARAM_SESSION_KEY.getCode()) {
			// setSessionKeyValid to false if we received the response that the session key is no longer valid
			facebookAccount.setSessionKeyValid(false);
		} else {
			logger.error("Did not receive a valid response for facebook.messages.getCount request, error message {}, error code {}",
					     handler.getErrorMessage(), handler.getErrorCode());
		}
        return newMessagesChanged;
	}
	
	private String generateFacebookRequest(List<String> params) {
	    StringBuffer signatureBuffer = new StringBuffer();
	    StringBuffer requestBuffer = new StringBuffer();
	    
		requestBuffer.append("http://api.facebook.com/restserver.php?");
		params.add("api_key=" + apiKey);
		params.add("call_id=" + System.currentTimeMillis());
		
	    // sort the list of parameters alphabetically
	    Collections.sort(params);
	    
	    // concatinate them in order
	    boolean firstParam = true;
	    for (String param : params) {
	        signatureBuffer.append(param);
	      
	        if (firstParam) {
	    	    firstParam = false;
	        } else {
	    	    requestBuffer.append("&");
	        }
	        requestBuffer.append(param);
	    }
	    
	    // concatinate the secret in the end of the signatureBuffer
	    signatureBuffer.append(this.secret);
	    
	    StringBuffer signature = new StringBuffer();
	    try {
	        // create an MD5 hash of the constructed buffer 	
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        for (byte b : md.digest(signatureBuffer.toString().getBytes())) { 
	            signature.append(Integer.toHexString((b & 0xf0) >>> 4));
	            signature.append(Integer.toHexString(b & 0x0f));
	        }
	    } catch (NoSuchAlgorithmException e) {
	        logger.error("No MD5 digest available!", e);
	    }
		
	    requestBuffer.append("&sig=");
		requestBuffer.append(signature);
		
		logger.debug("Created facebook web request {}", requestBuffer.toString());
	    return requestBuffer.toString();
	}
}
