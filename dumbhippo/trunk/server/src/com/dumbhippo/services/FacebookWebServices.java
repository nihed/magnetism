package com.dumbhippo.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoData;
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
	 * Return update time if the number of unread messages changed or the number of total messages 
	 * increased and some of them are unread messages.
	 * 
	 * @param facebookAccount
	 * @return update time if the set of unread messages changed
	 */
	public long updateMessageCount(FacebookAccount facebookAccount) {
	    // generate messages request using session from facebookAccount
		List<String> params = new ArrayList<String>();
		String methodName = "facebook.messages.getCount";
        params.add("method=" + methodName);
		params.add("session_key=" + facebookAccount.getSessionKey());

		String wsUrl = generateFacebookRequest(params);

		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(), wsUrl);
		
		if (handleErrorCode(facebookAccount, handler, methodName)) {
			return -1;
		}
		
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
			// new messages 
			facebookAccount.setTotalMessageCount(newTotal);		
			if ((newUnread != oldUnread) || ((newTotal > oldTotal) && (newUnread > 0)))  {
				long time = (new Date()).getTime();
				// we want the timestamp to correspond to the times when we signify change
				// in unread messages
				facebookAccount.setMessageCountTimestampAsLong(time);
				facebookAccount.setUnreadMessageCount(newUnread);
				return time; 
			}
		} else {
			logger.error("Did not receive a valid response for facebook.messages.getCount request, did not receive an error either."
					     + " Error message {}, error code {}", handler.getErrorMessage(), handler.getErrorCode());
		}
        return -1;
	}
	
	public FacebookEvent updateWallMessageCount(FacebookAccount facebookAccount) {
		List<String> params = new ArrayList<String>();
		String methodName = "facebook.wall.getCount";
        params.add("method=" + methodName);
		params.add("session_key=" + facebookAccount.getSessionKey());
		params.add("id=" + facebookAccount.getFacebookUserId());

		String wsUrl = generateFacebookRequest(params);

		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(), wsUrl);

		if (handleErrorCode(facebookAccount, handler, methodName)) {
			return null;
		}
		
		int newCount = handler.getWallMessageCount();
		int oldCount = facebookAccount.getWallMessageCount();
		
		if (newCount != -1) {
			facebookAccount.setWallMessageCount(newCount);
			// if it is the first time we get the wall message count, we do not want to create
			// an event about it
			if ((newCount > oldCount) && (oldCount != -1)) {
			        return new FacebookEvent(facebookAccount, FacebookEventType.NEW_WALL_MESSAGES_EVENT, 
			    	  	                     newCount - oldCount, (new Date()).getTime());
			}			
		} else {
			logger.error("Did not receive a valid response for facebook.wall.getCount request, did not receive an error either."
					     + " Error message {}, error code {}", handler.getErrorMessage(), handler.getErrorCode());
		}
		return null;
	}
	
	public long updatePokeCount(FacebookAccount facebookAccount) {
	    // generate messages request using session from facebookAccount
		List<String> params = new ArrayList<String>();
		String methodName = "facebook.pokes.getCount";
        params.add("method=" + methodName);
		params.add("session_key=" + facebookAccount.getSessionKey());

		String wsUrl = generateFacebookRequest(params);

		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(), wsUrl);

		if (handleErrorCode(facebookAccount, handler, methodName)) {
			return -1;
		}
		
		int newUnseen = handler.getUnseenPokeCount(); 
		int newTotal = handler.getTotalPokeCount();
		int oldUnseen = facebookAccount.getUnseenPokeCount();
		int oldTotal = facebookAccount.getTotalPokeCount();
		if ((newUnseen != -1) && (newTotal != -1)) {
			facebookAccount.setTotalPokeCount(newTotal);		
			if ((newUnseen != oldUnseen) || ((newTotal > oldTotal) && (newUnseen > 0)))  {
				facebookAccount.setUnseenPokeCount(newUnseen);
				if ((newUnseen > 0) || (oldUnseen != -1)) {
				    long time = (new Date()).getTime();
				    // we want the timestamp to correspond to the times when we signify change
				    // in unseen pokes
				    facebookAccount.setPokeCountTimestampAsLong(time);
				    return time; 
				}				
			}
		} else {
			logger.error("Did not receive a valid response for facebook.pokes.getCount request, did not receive an error either."
					     + " Error message {}, error code {}", handler.getErrorMessage(), handler.getErrorCode());
		}
		
        return -1;
	}
	
	public List<FacebookPhotoData> updateTaggedPhotos(FacebookAccount facebookAccount) {
		List<String> params = new ArrayList<String>();
		String methodName = "facebook.photos.getOfUser";
        params.add("method=" + methodName);
		params.add("session_key=" + facebookAccount.getSessionKey());
		params.add("id=" + facebookAccount.getFacebookUserId());
		
		String wsUrl = generateFacebookRequest(params);		
		
		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(facebookAccount), wsUrl);
		
		if (handleErrorCode(facebookAccount, handler, methodName)) {
			return null;
		}
		
		int newCount = handler.getTaggedPhotoCount();
		int oldCount = facebookAccount.getTaggedPhotos().size();

		if (newCount != oldCount) {
			// we do not want to go over all photos if the count did not change, even if there was an
			// equal number of photos added and removed, we we'll get all the new photos next time
			// the count changes
			// the reason we have this logic here and not in the FacebookTrackerBean is because it 
			// would be nice to change the code so that we parse through all the returned photos only
			// if the count did change
			return handler.getTaggedPhotos();
		} else if (!facebookAccount.getTaggedPhotosPrimed()) {
            // this covers the case when the first time we get tagged photos from facebook, the user has none 
			facebookAccount.setTaggedPhotosPrimed(true);
		}
		return null;
	}
	
	public Set<FacebookAlbumData> getModifiedAlbums(FacebookAccount facebookAccount) {
		List<String> params = new ArrayList<String>();
		String methodName = "facebook.photos.getAlbums"; 
        params.add("method=" + methodName);
		params.add("session_key=" + facebookAccount.getSessionKey());
		params.add("id=" + facebookAccount.getFacebookUserId());
		
		String wsUrl = generateFacebookRequest(params);		
		
		FacebookSaxHandler handler = parseUrl(new FacebookSaxHandler(facebookAccount), wsUrl);
		
		 Set<FacebookAlbumData> modifiedAlbums = new HashSet<FacebookAlbumData>();
		 
		if (handleErrorCode(facebookAccount, handler, methodName)) {
			return modifiedAlbums;
		}
		
		// because it is typical that the album would be created, and then photos would be 
		// added to it over a period of time, we want to check the modification date for each album,
		// rather than the count of albums
		// this assumes that the modification date for a newly added album will be later than the
		// last album modification date we are storing, so that's how we will learn than the album 
		// is new, rather than based on the total count of albums 
		for (FacebookAlbumData album : handler.getAlbums()) {
			if (album.getModifiedTimestampAsLong() > facebookAccount.getAlbumsModifiedTimestampAsLong()) {
				modifiedAlbums.add(album);
			}
		}
		
		if (modifiedAlbums.isEmpty() && facebookAccount.getAlbumsModifiedTimestampAsLong() < 0) {	 
			 // this covers the case when the first time we get albums from facebook, the user has none 
	    	facebookAccount.setAlbumsModifiedTimestampAsLong((new Date()).getTime());
	    }
		
        return modifiedAlbums;
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
	
	private boolean handleErrorCode(FacebookAccount facebookAccount, FacebookSaxHandler handler, String requestName) {
		if (handler.getErrorCode() > 0) {
		    if (handler.getErrorCode() == FacebookSaxHandler.FacebookErrorCode.API_EC_PARAM_SESSION_KEY.getCode()) {
			    // setSessionKeyValid to false if we received the response that the session key is no longer valid
			    facebookAccount.setSessionKeyValid(false);
		    } else {
			    logger.error("Did not receive a valid response for {} request, error message {}, error code {}",
					         new String[]{requestName, handler.getErrorMessage(), Integer.toString(handler.getErrorCode())});
		    }
			return true;
		}		
		return false;
	}
 }
