package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.Thumbnails;
import com.dumbhippo.persistence.ExternalAccountType;

public final class FlickrPhotos implements Thumbnails {
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

	public List<? extends Thumbnail> getThumbnails() {
		return photos;
	}

	public int getThumbnailCount() {
		return photos.size();
	}

	public int getThumbnailWidth() {
		return FlickrPhoto.THUMBNAIL_SIZE.getPixels();
	}

	public int getThumbnailHeight() {
		return FlickrPhoto.THUMBNAIL_SIZE.getPixels();
	}

	public int getTotalThumbnailItems() {
		return getTotal(); 
	}

	public String getTotalThumbnailItemsString() {
		return ExternalAccountType.FLICKR.formatThumbnailCount(getTotalThumbnailItems());
	}
}
