package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlickrPhotos {
	private List<FlickrPhoto> photos;
	private int page;
	private int pages;
	private int perPage;
	private int total;
	
	public FlickrPhotos() {
		photos = new ArrayList<FlickrPhoto>();
	}
	
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	public int getPages() {
		return pages;
	}
	public void setPages(int pages) {
		this.pages = pages;
	}
	public int getPerPage() {
		return perPage;
	}
	public void setPerPage(int perPage) {
		this.perPage = perPage;
	}
	
	public List<FlickrPhoto> getPhotos() {
		return Collections.unmodifiableList(photos);
	}
	
	public void addPhoto(FlickrPhoto photo) {
		photos.add(photo);
	}
	
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	
	@Override
	public String toString() {
		return "{" + photos.size() + " photos, page=" + page + " perPage=" + perPage +
		" pages=" + pages + " total=" + total + "}"; 
	}
}
