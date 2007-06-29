package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

public final class FlickrPhotosets implements FlickrPhotosetsView {
	
	private int total;
	private List<FlickrPhotosetView> sets;
	
	public FlickrPhotosets() {
		total = -1;
		sets = new ArrayList<FlickrPhotosetView>();
	}
	
	public List<? extends FlickrPhotosetView> getSets() {
		return sets;
	}
	
	void addSet(FlickrPhotoset set) {
		sets.add(set);
	}

	// when we get photosets from the web service we just count them to get the total, 
	// but when restoring from cache we potentially have the total stored
	public int getTotal() {
		if (total >= 0)
			return total;
		else
			return sets.size();
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public void setSets(List<? extends FlickrPhotosetView> photosets) {
		sets.clear();
		sets.addAll(photosets);
	} 
	
	@Override
	public String toString() {
		return "{FlickrPhotosets count=" + sets.size() + "}";
	}
}
