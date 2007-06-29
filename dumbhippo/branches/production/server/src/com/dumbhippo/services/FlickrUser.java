package com.dumbhippo.services;

import com.dumbhippo.StringUtils;

public final class FlickrUser {
	private String id;
	private String name;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		if (id.length() > FlickrWebServices.MAX_FLICKR_USER_ID_LENGTH)
			throw new IllegalArgumentException("Flickr returned a longer user id than we handle");				
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	static public String getProfileUrl(String id) {
		return "http://www.flickr.com/people/" + StringUtils.urlEncode(id);
	}
	
	static public String getPhotosUrl(String id) {
		return "http://www.flickr.com/photos/" + StringUtils.urlEncode(id);
	}
	
	public String getPhotosUrl() {
		return getPhotosUrl(id);
	}
	
	public String getProfileUrl() {
		return getProfileUrl(id);
	}
	
	@Override
	public String toString() {
		return "{id=" + id + " name='" + name + "' profile=" + getProfileUrl() + "}";
	}
}
