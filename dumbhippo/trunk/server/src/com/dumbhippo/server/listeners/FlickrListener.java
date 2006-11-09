package com.dumbhippo.server.listeners;

import java.util.List;

import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotosetView;

public interface FlickrListener {
	public void onMostRecentFlickrPhotosChanged(String flickrId, List<FlickrPhotoView> recentPhotos);
	public void onFlickrPhotosetsChanged(String flickrId, List<FlickrPhotosetView> allPhotosets);
}
