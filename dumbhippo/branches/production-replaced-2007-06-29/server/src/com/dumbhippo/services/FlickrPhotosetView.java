package com.dumbhippo.services;

import com.dumbhippo.Thumbnails;

/**
 * A read-only minimal subset of FlickrPhotoset that we pass around in most places
 * instead of a concrete implementation
 * 
 * @author Havoc Pennington
 *
 */
public interface FlickrPhotosetView {

	public String getDescription();

	public String getTitle();

	public String getId();

	public int getPhotoCount();

	public String getUrl(String ownerId);
	
	public Thumbnails getThumbnails();
}
