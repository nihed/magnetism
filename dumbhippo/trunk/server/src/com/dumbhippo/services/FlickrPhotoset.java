package com.dumbhippo.services;

import com.dumbhippo.Thumbnails;

public final class FlickrPhotoset implements FlickrPhotosetView {
	private String id;
	private FlickrPhoto primaryPhoto;
	private int photoCount;
	private String title;
	private String description;
	private FlickrPhotos photos;
	
	public FlickrPhotoset() {
		
	}	
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getPhotoCount() {
		return photoCount;
	}
	public void setPhotoCount(int photoCount) {
		this.photoCount = photoCount;
	}
	public FlickrPhoto getPrimaryPhoto() {
		if (primaryPhoto == null && photos != null) {
			// when we get a list of photos, flickr doesn't identify the 
			// primary photo up front, instead it flags it in the list
			for (FlickrPhoto photo : photos.getPhotos()) {
				if (photo.isPrimary()) {
					primaryPhoto = photo;
					return primaryPhoto;
				}
			}
		}
		
		return primaryPhoto;
	}

	public void setPrimaryPhoto(FlickrPhoto primaryPhoto) {
		this.primaryPhoto = primaryPhoto;
	}
	
	public boolean getHasPhotos() {
		return this.photos != null;
	}
	
	public void setPhotos(FlickrPhotos photos) {
		this.photos = photos;
	}
	
	public FlickrPhotos getPhotos() {
		return photos;
	}
	
	// See http://www.flickr.com/services/api/misc.urls.html 
	// for info on urls
	public String getUrl(String ownerId) {
		StringBuilder sb = new StringBuilder("http://www.flickr.com/photos/");
		sb.append(ownerId);
		sb.append("/sets/");
		sb.append(getId());
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "{FlickrPhotoset id=" + id + " title=" + title + " desc=" + description +
		" primary=" + getPrimaryPhoto() + " hasPhotos=" + getHasPhotos() + "}";
	}

	public Thumbnails getThumbnails() {
		return getPhotos();
	}
}
