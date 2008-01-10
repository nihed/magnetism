package com.dumbhippo.services;

import java.util.List;

import com.dumbhippo.Thumbnails;

/** An interface that exports only the aspects of FlickrPhotos that are really useful outside 
 * the package.
 */
public interface FlickrPhotosView extends Thumbnails {

	public List<? extends FlickrPhotoView> getPhotos();

	public int getTotal();
	
	public void setPhotos(List<? extends FlickrPhotoView> photos);
}
