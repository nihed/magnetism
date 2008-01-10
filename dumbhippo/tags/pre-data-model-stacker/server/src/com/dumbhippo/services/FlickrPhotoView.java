package com.dumbhippo.services;

import com.dumbhippo.Thumbnail;

/** 
 * Interface for the aspects of a Flickr photo needed to display its
 * thumbnail, link the thumbnail to something, and put a tooltip on the 
 * thumbnail. The implementing classes such as FlickrPhoto may have more 
 * fields available, but many of those fields are optional (only returned by 
 * certain web services requests and not others).
 * 
 * @author Havoc Pennington
 */
public interface FlickrPhotoView extends Thumbnail {

	public String getId();

	public String getSecret();

	public String getServer();

	public String getTitle();

	// See http://www.flickr.com/services/api/misc.urls.html 
	// for info on urls
	public String getUrl(FlickrPhotoSize size);
}
