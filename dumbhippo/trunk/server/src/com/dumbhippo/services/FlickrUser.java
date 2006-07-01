package com.dumbhippo.services;

public final class FlickrUser {
	private String id;
	private String name;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPhotosUrl() {
		return "http://www.flickr.com/photos/" + id;
	}
	
	public String getProfileUrl() {
		return "http://www.flickr.com/people/" + id;
	}
	
	@Override
	public String toString() {
		return "{id=" + id + " name='" + name + "' profile=" + getProfileUrl() + "}";
	}
}
