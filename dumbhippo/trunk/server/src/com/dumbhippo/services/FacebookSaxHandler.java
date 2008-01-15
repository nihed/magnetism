package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;

public class FacebookSaxHandler extends EnumSaxHandler<FacebookSaxHandler.Element>{
	static private final Logger logger = GlobalSetup.getLogger(FacebookSaxHandler.class);

	enum Element {			
		// facebook.auth.getSession
		session_key,
		uid,
		
		// facebook.notifications.get
		messages, 
		pokes,
		unread,
		most_recent,
		
		// facebook.users.getInfo, we get wall_count, first_name, and last_name fields from it
		wall_count,
		first_name,
		last_name,
		
		// general for facebook.photos.get and facebook.photos.getAlbums
		created,
		aid,
		
		// facebook.photos.get
		photos_get_response,
		photo,
		link,
		src_small, 
		caption,
		
		// facebook.photos.getAlbums
		photos_getAlbums_response,
		album,
		cover_pid,
		name, 
		modified,
		description,
		location,
		
		// facebook.update.decodeIDs
		id_map,
		old_id,
		new_id,
		
		// friends.getAppUsers
		friends_getAppUsers_response,
		
		// error message
		error_code,
        error_msg,
            
		IGNORED // an element we don't care about
	}
	
	public enum FacebookErrorCode {
		API_EC_UNKNOWN(1),
		API_EC_SERVICE(2),
		API_EC_TOO_MANY_CALLS(4),
		API_EC_BAD_IP(5),
		API_EC_PARAM_INVALID(100),
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
	private int mostRecentMessageId;
	private int wallMessageCount;
	private String firstName;
	private String lastName;
	private int unseenPokeCount;
	private int mostRecentPokeId;
	private int errorCode;
	private String errorMessage;
	private List<FacebookPhotoData> taggedPhotos;
	private boolean gettingTaggedPhotos;
	private List<FacebookAlbumData> albums;
	private boolean gettingAlbums;
	private List<Pair<String, String>> idPairs;
	private boolean gettingFriendAppUsers;
	private List<String> appUsersUids;

	FacebookSaxHandler() {
		this(null);	
	}

	FacebookSaxHandler(FacebookAccount facebookAccount) {
		super(Element.class, Element.IGNORED);
		this.facebookAccount = facebookAccount;
		unreadMessageCount = -1;
		mostRecentMessageId = -1;
		wallMessageCount = -1;
		firstName = "";
		lastName = "";
		unseenPokeCount = -1;
        mostRecentPokeId = -1;
		errorCode = -1;
		taggedPhotos = new ArrayList<FacebookPhotoData>();
		gettingTaggedPhotos = false;
		albums = new ArrayList<FacebookAlbumData>();
		gettingAlbums = false;
		idPairs = new ArrayList<Pair<String, String>>();
		gettingFriendAppUsers = false;
		appUsersUids = new ArrayList<String>();
	}
	
	private FacebookPhotoData currentFacebookPhotoData() {
		if (taggedPhotos.size() > 0)
			return taggedPhotos.get(taggedPhotos.size() - 1);
		else
			return null;
	}

	private FacebookAlbumData currentFacebookAlbumData() {
		if (albums.size() > 0)
			return albums.get(albums.size() - 1);
		else
			return null;
	}
	
	private Pair<String, String> getCurrentIdPair() {
		if (idPairs.size() > 0)
			return idPairs.get(idPairs.size() - 1);
		else
			return null;	
    }

	@Override
	protected void openElement(Element c) throws SAXException {
		if (c == Element.photos_get_response) {
			gettingTaggedPhotos = true; 
		} else if (c == Element.photos_getAlbums_response) {
			gettingAlbums = true;
	    } else if ((c == Element.photo) && gettingTaggedPhotos) {
			FacebookPhotoData photo = new FacebookPhotoData();
			taggedPhotos.add(photo);
		} else if ((c == Element.album) && gettingAlbums) {
			FacebookAlbumData album = new FacebookAlbumData();
			album.setFacebookAccount(facebookAccount);
			album.getCoverPhoto().setFacebookAccount(facebookAccount);
			albums.add(album);
		} else if (c == Element.id_map) {
			Pair<String, String> idPair = new Pair<String, String>(null, null);
			idPairs.add(idPair);
		} else if (c == Element.friends_getAppUsers_response) {
			gettingFriendAppUsers = true;
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
		} else if (c == Element.uid) {
			if (gettingFriendAppUsers)
				appUsersUids.add(currentContent);
			else
			    facebookUserId = currentContent;
		} else if (c == Element.unread) {
			if (parent() == Element.messages) {
                unreadMessageCount = parseFacebookCount(c, currentContent); 
			} else if (parent() == Element.pokes) {
				unseenPokeCount = parseFacebookCount(c, currentContent); 					
			}
		} else if (c == Element.most_recent) {
			if (parent() == Element.messages) {
                mostRecentMessageId = parseFacebookCount(c, currentContent); 
			} else if (parent() == Element.pokes) {
				mostRecentPokeId = parseFacebookCount(c, currentContent); 				
			}
		} else if (c == Element.wall_count) {
			wallMessageCount = parseFacebookCount(c, currentContent); 
		} else if (c == Element.first_name) {
			firstName = currentContent;
		} else if (c == Element.last_name) {
			lastName = currentContent;
		} else if (c == Element.link) {
			if (gettingTaggedPhotos) {
			    currentFacebookPhotoData().setLink(currentContent);
			} else if (gettingAlbums) {
				// currentFacebookAlbumData().getCoverPhoto().setLink(currentContent);
			}
		} else if (c == Element.src_small) {
			if (gettingTaggedPhotos) {
			    currentFacebookPhotoData().setSource(currentContent);
			} else if (gettingAlbums) {
				// currentFacebookAlbumData().getCoverPhoto().setSource(currentContent);
			}
		} else if (c == Element.caption) {
			if (gettingTaggedPhotos) {
			    currentFacebookPhotoData().setCaption(currentContent);
			} else if (gettingAlbums) {
				// currentFacebookAlbumData().getCoverPhoto().setCaption(currentContent);
			}			    
		} else if (c == Element.created) {
		    long createdTimestamp = parseFacebookDate(c, currentContent);	
			if (gettingTaggedPhotos) {
			    currentFacebookPhotoData().setCreatedTimestamp(new Date(createdTimestamp));
			} else if (gettingAlbums) {
			    // currentFacebookAlbumData().setCreatedTimestampAsLong(createdTimestamp);				
			}
		} else if (c == Element.modified) { 
			long modifiedTimestamp = parseFacebookDate(c, currentContent);
			currentFacebookAlbumData().setModifiedTimestampAsLong(modifiedTimestamp);
		} else if (c == Element.aid) {
			if (gettingTaggedPhotos) {
			    currentFacebookPhotoData().setAlbumId(currentContent);
			} else if (gettingAlbums) {
				currentFacebookAlbumData().setAlbumId(currentContent);
			}
		} else if (c == Element.cover_pid) {
			// FIXME: this call encrypts the id and we'll never see it again, 
			// while what we'll really want if we'll be doing anything with user's 
			// albums is to get the photo for the cover using this id
			currentFacebookAlbumData().getCoverPhoto().setPhotoId(currentContent);
		} else if (c == Element.name) {
			currentFacebookAlbumData().setName(currentContent);
		} else if (c == Element.description) {
			currentFacebookAlbumData().setDescription(currentContent);	
		} else if (c == Element.location) {
			currentFacebookAlbumData().setLocation(currentContent);
		} else if (c == Element.old_id) {
			getCurrentIdPair().setFirst(currentContent); 
	    } else if (c == Element.new_id) {
		    getCurrentIdPair().setSecond(currentContent);
	    } else if (c == Element.error_code) {
			errorCode = parseFacebookCount(c, currentContent);
			logger.debug("Parsed out error code {}", errorCode);
		} else if (c == Element.error_msg) {
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

	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}

	public int getMostRecentMessageId() {
		return mostRecentMessageId;
	}
	
	public int getWallMessageCount() {
		return wallMessageCount;
	}

	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public int getUnseenPokeCount() {
		return unseenPokeCount;
	}

	public int getMostRecentPokeId() {
		return mostRecentPokeId;
	}
	
	public int getTaggedPhotoCount() {
		return taggedPhotos.size();
	}
	
	public List<FacebookPhotoData> getTaggedPhotos() {
		return taggedPhotos;
	}
	
	public List<FacebookAlbumData> getAlbums() {
		return albums;
	}
	
	public List<Pair<String, String>> getIdPairs() {
		return idPairs;
	}
	
	public List<String> getFriendAppUsers() {
		return appUsersUids;
	}
}
