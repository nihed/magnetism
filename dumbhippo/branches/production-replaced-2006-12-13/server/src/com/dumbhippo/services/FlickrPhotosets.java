package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

public final class FlickrPhotosets {
	private List<FlickrPhotoset> sets;
	
	FlickrPhotosets() {
		sets = new ArrayList<FlickrPhotoset>();
	}
	
	public List<FlickrPhotoset> getSets() {
		return sets;
	}
	
	void addSet(FlickrPhotoset set) {
		sets.add(set);
	}
	
	@Override
	public String toString() {
		return "{FlickrPhotosets count=" + sets.size() + "}";
	}
}
