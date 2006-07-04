package com.dumbhippo.persistence;

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
			return "http://myspace.com/" + handle;
		}
	},
	FLICKR("Flickr") {
		@Override
		public String getLink(String handle, String extra) {
			// handle is "NSID" extra is flickr email address
			return FlickrUser.getProfileUrl(handle);
		}
	},
	LINKED_IN("LinkedIn")  {
		@Override
		public String getLink(String handle, String extra) {
			return "http://www.linkedin.com/in/" + handle;
		}
	},
	WEBSITE("Website")  {
		@Override
		public String getLink(String handle, String extra) {
			// the "website" thing is just an url we know nothing further about
			return handle;
		}
	},
	FACEBOOK("Facebook")  {
		@Override
		public String getLink(String handle, String extra) {
			// the handle here is a numeric ID 
			return "http://www.facebook.com/profile.php?id=" + handle;
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
}
