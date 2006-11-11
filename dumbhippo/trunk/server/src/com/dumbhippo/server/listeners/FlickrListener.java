package com.dumbhippo.server.listeners;

import java.util.List;

import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.services.FlickrPhotoView;

public interface FlickrListener {
	public void onMostRecentFlickrPhotosChanged(String flickrId, List<FlickrPhotoView> recentPhotos);
	public void onFlickrPhotosetCreated(FlickrPhotosetStatus photosetStatus);
	public void onFlickrPhotosetChanged(FlickrPhotosetStatus photosetStatus);	
}
