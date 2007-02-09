package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.services.FlickrPhotosView;

public interface FlickrListener {
	public void onMostRecentFlickrPhotosChanged(String flickrId, FlickrPhotosView photosView);
	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus);
	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus);	
}
