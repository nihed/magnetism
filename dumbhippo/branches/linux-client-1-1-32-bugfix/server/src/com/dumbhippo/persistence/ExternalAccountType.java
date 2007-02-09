package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;

import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FlickrUser;
import com.dumbhippo.services.YouTubeWebServices;
import com.dumbhippo.services.LastFmWebServices;

import org.slf4j.Logger;
import com.dumbhippo.GlobalSetup;

/**
 * This enum goes in the database, so don't change values without migrating the db
 * 
 * @author Havoc Pennington
 *
 */
public enum ExternalAccountType {
	MYSPACE("MySpace") { // 0
		@Override
		public String getLink(String handle, String extra) {
			// handle is myspace name, extra is friend id number
			return getSiteLink() + "/" + StringUtils.urlEncode(handle);
		}
		
		@Override
		public String getSiteLink() {
		    return "http://myspace.com";	
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return handle;
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username";
	    }	
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (handle.length() > 40)
					throw new ValidationException("MySpace name too long: " + handle);
				if (!handle.matches("^[\\p{Alnum}]+$"))
					throw new ValidationException("Invalid MySpace name: " + handle);
			}
			return handle;
		}
		// friend ID
		@Override
		public String canonicalizeExtra(String extra) throws ValidationException {
			extra = super.canonicalizeExtra(extra);
			if (extra != null) {
				try {
					long val = Long.parseLong(extra);
					extra = Long.toString(val); // a little extra paranoia
				} catch (NumberFormatException e) {
					throw new ValidationException("Friend id is not a number: " + extra);
				}
			}
			return extra;
		}

		@Override
		public boolean requiresExtraIfLoved() {
			return true;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null && extra != null;
		}
	},
	FLICKR("Flickr") { // 1
		@Override
		public String getLink(String handle, String extra) {
			// handle is "NSID" extra is flickr email address
			return FlickrUser.getPhotosUrl(handle);
		}
		
		@Override
		public String getSiteLink() {
		    return "http://www.flickr.com";	
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "My Photos";
		}

		@Override
	    public String getSiteUserInfoType() {
	    	return "email used for";
	    }
		
		@Override
	    public boolean isInfoTypeProvidedBySite() {
	    	return false;
	    }
	    
		@Override
		public String canonicalizeExtra(String extra) throws ValidationException {
			extra = super.canonicalizeExtra(extra);
			if (extra != null)
				return EmailResource.canonicalize(extra);
			else
				return null;
		}

		@Override
		public String formatThumbnailCount(int count) {
			if (count == 1)
				return count + " photo";
			else
				return count + " photos";
		}

		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	},
	LINKED_IN("LinkedIn")  { // 2
		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/in/" + StringUtils.urlEncode(handle);
		}
		
		@Override
		public String getSiteLink() {
			return "http://www.linkedin.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "My Profile";
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }		
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid LinkedIn username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}		
	},
	WEBSITE("Website")  { // 3
		@Override
		public String getLink(String handle, String extra) {
			// the "website" thing is just an url we know nothing further about
			return handle;
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			// prettier without the http://
			if (handle.startsWith("http://"))
				return handle.substring("http://".length());
			else
				return handle;
		}
		
		@Override
		public String getIconName() {
		    return "homepage_icon.png";	
		}
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				try {
					handle = new URL(handle).toExternalForm();
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid URL: " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
		
		@Override
		public boolean isSupported() {
			return false;
		}
	},
	FACEBOOK("Facebook")  { // 4
		@Override
		public String getLink(String handle, String extra) {
			logger.warn("getLink() in ExternalAccountType does not provide a link to a Facebook profile based on a handle and an extra.");
			return getSiteLink();
		}
		
		@Override
		public String getSiteLink() {
		    return "http://www.facebook.com";	
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "My Profile";
		}		
        // id in FacebookAccount
		@Override
		public String canonicalizeExtra(String extra) throws ValidationException {
			extra = super.canonicalizeExtra(extra);
			if (extra != null) {
				try {
					long val = Long.parseLong(extra);
					extra = Long.toString(val); // a little extra paranoia
				} catch (NumberFormatException e) {
					throw new ValidationException("FacebookAccount id is not a number: " + extra);
				}
			}
			return extra;
		}

		@Override
		public boolean requiresHandleIfLoved() {
			return false;
		}

		@Override
		public boolean requiresExtraIfLoved() {
			return true;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return extra != null;
		}
		
		@Override
		public boolean isSupported() {
			return false;
		}
	},
	ORKUT("Orkut")  { // 5
		@Override
		public String getLink(String handle, String extra) {
			throw new UnsupportedOperationException("add orkut support");
		}
		@Override
		public String getLinkText(String handle, String extra) {
			throw new UnsupportedOperationException("Not implemented yet");
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
		
		@Override
		public boolean isSupported() {
			return false;
		}
	},
	YOUTUBE("YouTube")  { // 6
		/* YouTube RSS feeds
			http://www.youtube.com/rssls
			feed://www.youtube.com/rss/user/$username/videos.rss
		*/

		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/user/" + StringUtils.urlEncode(handle);
		}
		
		@Override 
		public String getSiteLink() {
		    return "http://www.youtube.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return handle;
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (handle.length() > YouTubeWebServices.MAX_YOUTUBE_USERNAME_LENGTH)
					throw new ValidationException("YouTube usernames have a maximum length of " + YouTubeWebServices.MAX_YOUTUBE_USERNAME_LENGTH);
				
				// This is determined from the YouTube signin form which throws this error if you put in 
				// non-letters or non-digits
				if (!StringUtils.isAlphanumeric(handle))
					throw new ValidationException("YouTube usernames can only have letters and digits");
				
				// As extra paranoia, be sure we can use the username in an url
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid YouTube username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}

		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	},
	XANGA("Xanga")  { // 7
		@Override
		public String getLink(String handle, String extra) {
			throw new UnsupportedOperationException("add xanga support");
		}
		@Override
		public String getLinkText(String handle, String extra) {
			throw new UnsupportedOperationException("Not implemented yet");
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}

		@Override
		public boolean isSupported() {
			return false;
		}
	},
	BLOG("Blog") { // 8
		@Override
		public String getLink(String handle, String extra) {
			// handle is the blog url (human-readable, not the feed url)
			return handle;
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			// prettier without the http://
			if (handle.startsWith("http://"))
				return handle.substring("http://".length());
			else
				return handle;
		}
		
		@Override
		public String getIconName() {
		    return "blog_icon.png";	
		}
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				try {
					handle = new URL(handle).toExternalForm();
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid URL: " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
		
		@Override
		public boolean isSupported() {
			return false;
		}
	},
	RHAPSODY("Rhapsody") { // 9
		@Override
		public String getLink(String handle, String extra) {
			// handle is the rhapUserId in the feed url
			// since there do not seem to exist public user profiles on Rhapsody,
			// the best we can offer for the person's Rhapsody account is this feed url
			return "http://feeds.rhapsody.com/user-track-history.rss?rhapUserId=" + handle + "&userName=I";
		}
		
		@Override
		public String getSiteLink() {
		    return "http://www.rhapsody.com";	
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "Recent Tracks RSS";
		}
		
		@Override
		public String getSiteUserInfoType() {
			return "\u201CRecently Played Tracks\u201D RSS feed URL";			
		}
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (!(StringUtils.isAlphanumeric(handle) && (handle.length() == 32)))
					throw new ValidationException("Rhapsody rhapUserId can only have letters and digits and be 32 characters long");
				
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Rhapsody rhapUserId '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
		
		@Override
		public boolean isAffectedByMusicSharing() {
			return true;
		}
	},
	LASTFM("Last.fm")  { // 10
		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/user/" + StringUtils.urlEncode(handle) + "/";
		}
		
		@Override 
		public String getSiteLink() {
		    return "http://www.last.fm";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return handle;
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (handle.length() > LastFmWebServices.MAX_USERNAME_LENGTH)
					throw new ValidationException("Last.fm usernames have a maximum length of " + LastFmWebServices.MAX_USERNAME_LENGTH);
				
				if (!handle.matches("[-_A-Za-z0-9.]+"))
					throw new ValidationException("Last.fm usernames can only have letters, digits, dash, dot and underscore (- . _)");
				
				// As extra paranoia, be sure we can use the username in an url
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Last.fm username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}

		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
		
		@Override
		public String getIconName() {
			return "favicon_lastfm.png";
		}		
		
		@Override
		public boolean isAffectedByMusicSharing() {
			return true;
		}
	}, 
	DELICIOUS("del.icio.us") { // 11
		@Override
		public String getIconName() {
			return "favicon_delicious.png";
		}
		
		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/" + StringUtils.urlEncode(handle);
		}
		
		@Override
		public String getSiteLink() {
			return "http://del.icio.us";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "My Favorites";
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				// According to del.icio.us login form, they allow a-z 0-9 underscore and period.
				if (!StringUtils.isAlphanumericOrInSet(handle, "_."))
					throw new ValidationException("del.icio.us usernames can only have letters, digits, underscores, and periods");
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid del.icio.us username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}		
	},
	TWITTER("Twitter") { // 12
		@Override
		public String getIconName() {
			return "favicon_twitter.png";
		}
		
		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/" + StringUtils.urlEncode(handle);
		}
		
		@Override
		public String getSiteLink() {
			return "http://twitter.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "My Updates";
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Twitter username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}		
	},
	DIGG("Digg") { // 13
		@Override
		public String getIconName() {
			return "favicon_digg.png";
		}
		
		@Override
		public String getLink(String handle, String extra) {
			return "http://digg.com/users/" + StringUtils.urlEncode(handle) + "/dugg";
		}
		
		@Override
		public String getSiteLink() {
			return "http://digg.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "Dugg by " + handle;
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				// 4-15 chars no spaces
				if (handle.length() < 4)
					throw new ValidationException("Too short to be a Digg username '" + handle + "'");
				if (handle.length() > 15)
					throw new ValidationException("Too long to be a Digg username '" + handle + "'");
				if (handle.contains(" "))
					throw new ValidationException("Digg username contains a space '" + handle + "'");
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Digg username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	},
	REDDIT("Reddit") { // 14
		@Override
		public String getIconName() {
			return "favicon_reddit.png";
		}
		
		@Override
		public String getLink(String handle, String extra) {
			return "http://reddit.com/user/" + StringUtils.urlEncode(handle) + "/";
		}
		
		@Override
		public String getSiteLink() {
			return "http://reddit.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "Reddit Activity";
		}
		
		@Override
	    public String getSiteUserInfoType() {
	    	return "username or profile URL";
	    }
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				// FIXME Reddit doesn't say what their username limits are, and the error message
				// is just "invalid username" if it doesn't like one, so it's tough to know
				// what to validate
				try {
					new URL(getLink(handle, null));
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Reddit username '" + handle + "': " + e.getMessage());
				}
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	}, 
	NETFLIX("Netflix") { // 15
		@Override
		public String getIconName() {
			return "favicon_netflix.png";
		}
		
		@Override
		public String getLink(String handle, String extra) {
			return getSiteLink() + "/Invitation";
		}
		
		@Override
		public String getSiteLink() {
			return "http://netflix.com";
		}
		
		@Override
		public String getLinkText(String handle, String extra) {
			return "Find this person on Netflix";
		}
		
		@Override
		public String getSiteUserInfoType() {
			return "\u201CMovies At Home\u201D RSS feed URL";			
		}
		
		@Override
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				// the user id for RSS feeds consists of "P" followed by 34 digits
				if (!((handle.indexOf("P") == 0) && (handle.substring(1).matches("[0-9]+") && handle.length() == 35)))
					throw new ValidationException("Netflix user id for RSS feeds should be 'P' followed by 34 digits");
				
				try {
					new URL("http://rss.netflix.com/AtHomeRSS?id=" + handle);
				} catch (MalformedURLException e) {
					throw new ValidationException("Invalid Netflix user id handle '" + handle + "': " + e.getMessage());
				}
			}			
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}		
	};
	
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountType.class);	
	
	private String siteName;
	
	ExternalAccountType(String siteName) {
		this.siteName = siteName;
	}
	
	/**
	 * Gets the human-readable name for what this account is on
	 * 
	 * @return name of the site the account is on
	 */
	public String getSiteName() {
		return siteName;
	}
	
	public String getIconName() {
		return "favicon_" + siteName.toLowerCase() + ".png";
	}
	
	abstract public String getLink(String handle, String extra);
	
	abstract public String getLinkText(String handle, String extra);
	
	public String getSiteLink() {
	    return "";	
	}
	
	public String formatThumbnailCount(int count) {
		if (count == 1)
			return count + " item";
		else
			return count + " items";
	}
	
	// do we have the info we need to be able to use the account - people can 
	// register love/hate or a quip without setting their account name
	abstract public boolean getHasAccountInfo(String handle, String extra);
	
	public String canonicalizeHandle(String handle) throws ValidationException {
		if (handle != null) {
			handle = handle.trim();
			if (handle.length() == 0)
				handle = null;
		}
		return handle;
	}
	
	public boolean requiresHandleIfLoved() {
		return true;
	}
	
	public String canonicalizeExtra(String extra) throws ValidationException {
		if (extra != null) {
			extra = extra.trim();
			if (extra.length() == 0)
				extra = null;
		}		
		return extra;
	}

	public boolean requiresExtraIfLoved() {
		return false;
	}
	
    public boolean isSupported() {
    	return true;
    }
    
    public String getName() {
    	return name();
    }
    
    // the type of the user identifying info we ask the user to enter to identify their
    // account to us
    public String getSiteUserInfoType() {
    	return "user info";
    }
    
    // how we should talk about the identifying info, is it something the site provides you with
    // (i.e. a username) or is it something you provide the site with (i.e. your e-mail)
    public boolean isInfoTypeProvidedBySite() {
    	return true;
    }
    
    public boolean isAffectedByMusicSharing() {
    	return false;
    }
}
