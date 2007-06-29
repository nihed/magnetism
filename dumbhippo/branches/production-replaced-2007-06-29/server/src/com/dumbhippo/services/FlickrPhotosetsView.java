package com.dumbhippo.services;

import java.util.List;

public interface FlickrPhotosetsView {

	public List<? extends FlickrPhotosetView> getSets();

	public int getTotal();

	public void setSets(List<? extends FlickrPhotosetView> photosets);
}