package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;

import com.dumbhippo.StringUtils;
import com.dumbhippo.services.FlickrUser;

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
			return "http://myspace.com/" + StringUtils.urlEncode(handle);
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
			extra = super.canonicalizeHandle(extra);
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
	},
	FLICKR("Flickr") {
		@Override
		public String getLink(String handle, String extra) {
			// handle is "NSID" extra is flickr email address
			return FlickrUser.getProfileUrl(handle);
		}
		@Override
		public String canonicalizeExtra(String extra) throws ValidationException {
			extra = super.canonicalizeExtra(extra);
			if (extra != null)
				return EmailResource.canonicalize(extra);
			else
				return null;
		}
		
	},
	LINKED_IN("LinkedIn")  {
		@Override
		public String getLink(String handle, String extra) {
			return "http://www.linkedin.com/in/" + StringUtils.urlEncode(handle);
		}
	},
	WEBSITE("Website")  {
		@Override
		public String getLink(String handle, String extra) {
			// the "website" thing is just an url we know nothing further about
			return handle;
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
	},
	FACEBOOK("Facebook")  {
		@Override
		public String getLink(String handle, String extra) {
			// the handle here is a numeric ID 
			return "http://www.facebook.com/profile.php?id=" + StringUtils.urlEncode(handle);
		}
	},
	ORKUT("Orkut")  {
		@Override
		public String getLink(String handle, String extra) {
			throw new UnsupportedOperationException("add orkut support");
		}
	},
	YOUTUBE("YouTube")  {
		@Override
		public String getLink(String handle, String extra) {
			throw new UnsupportedOperationException("add youtube support");
		}
	},
	XANGA("Xanga")  {
		@Override
		public String getLink(String handle, String extra) {
			throw new UnsupportedOperationException("add xanga support");
		}
	};
	
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
	
	abstract public String getLink(String handle, String extra);
	
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
