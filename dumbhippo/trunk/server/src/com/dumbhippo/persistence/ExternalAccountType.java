package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;

import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FlickrUser;
import com.dumbhippo.services.LastFmWebServices;
import com.dumbhippo.services.YouTubeWebServices;

import org.slf4j.Logger;
import com.dumbhippo.GlobalSetup;

/**
 * This enum goes in the database, so don't change values without migrating the db
 * 
 * @author Havoc Pennington
 *
 */
public enum ExternalAccountType {
	MYSPACE("MySpace") {
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
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null && extra != null;
		}
	},
	FLICKR("Flickr") {
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
	LINKED_IN("LinkedIn")  {
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
	WEBSITE("Website")  {
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
	},
	FACEBOOK("Facebook")  {
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
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	},
	ORKUT("Orkut")  {
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
	},
	YOUTUBE("YouTube")  {
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
	XANGA("Xanga")  {
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
	},
	BLOG("Blog") {
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
	},
	RHAPSODY("Rhapsody") {
		@Override
		public String getLink(String handle, String extra) {
			// handle is the rhapUserId in the feed url
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
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (handle.trim().length() == 0)
					throw new ValidationException("empty rhapUserId in Rhapsody URL");
			}
			return handle;
		}
		
		@Override
		public boolean getHasAccountInfo(String handle, String extra) {
			return handle != null;
		}
	},
	LASTFM("Last.fm")  {
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
		public String canonicalizeHandle(String handle) throws ValidationException {
			handle = super.canonicalizeHandle(handle);
			if (handle != null) {
				if (handle.length() > LastFmWebServices.MAX_USERNAME_LENGTH)
					throw new ValidationException("Last.fm usernames have a maximum length of " + LastFmWebServices.MAX_USERNAME_LENGTH);
				
				if (!StringUtils.isAlphanumeric(handle))
					throw new ValidationException("Last.fm usernames can only have letters and digits");
				
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
	public String canonicalizeExtra(String extra) throws ValidationException {
		if (extra != null) {
			extra = extra.trim();
			if (extra.length() == 0)
				extra = null;
		}		
		return extra;
	}
}
