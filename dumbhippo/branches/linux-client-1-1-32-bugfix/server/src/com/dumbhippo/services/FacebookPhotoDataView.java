package com.dumbhippo.services;

import java.util.Date;

import com.dumbhippo.Thumbnail;

/**
 * A read-only minimal subset of FacebookPhotoData that we pass around in most places
 * instead of a concrete implementation
 *
 */
public interface FacebookPhotoDataView extends Thumbnail {

	public String getLink();

	public String getSource();

	public String getCaption();

	public Date getCreatedTimestamp();
	
	public String getAlbumId();
	
	public String getPhotoId();
}
