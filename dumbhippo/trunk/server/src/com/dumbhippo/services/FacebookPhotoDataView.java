package com.dumbhippo.services;

import java.util.Date;

/**
 * A read-only minimal subset of FacebookPhotoData that we pass around in most places
 * instead of a concrete implementation
 *
 */
public interface FacebookPhotoDataView {

	public String getLink();

	public String getSource();

	public String getCaption();

	public Date getCreatedTimestamp();
	
	public String getAlbumId();
	
	public String getPhotoId();
}
