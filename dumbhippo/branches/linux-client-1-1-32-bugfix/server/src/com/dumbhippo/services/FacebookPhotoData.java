package com.dumbhippo.services;

import java.util.Date;

public class FacebookPhotoData implements FacebookPhotoDataView {

	public static int FACEBOOK_THUMB_SIZE = 100;
	
	private String link;
	private String source;
	private String caption;
	private Date createdTimestamp;
	private String albumId;
	private String photoId;
	
	public String getAlbumId() {
		return albumId;
	}
	
	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}
	public String getCaption() {
		return caption;
	}
	
	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	public Date getCreatedTimestamp() {
		return createdTimestamp;
	}
	
	public void setCreatedTimestamp(Date createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}
	
	public String getLink() {
		return link;
	}
	
	public void setLink(String link) {
		this.link = link;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}	
	
	public String getThumbnailSrc() {
		return getSource() + "&size=thumb";
	}
	
	public String getThumbnailHref() {
		return getLink();
	}
	
	public String getThumbnailTitle() {
		return getCaption();
	}
	
	public int getThumbnailWidth() {
		return FACEBOOK_THUMB_SIZE;
	}
	
	public int getThumbnailHeight() {
		return FACEBOOK_THUMB_SIZE;
	}
	
	@Override
	public String toString() {
		return "{FacebookPhotoData photoId=" + photoId + " link=" + link + " source=" + source +
		" caption=" + caption + " createdTimestamp" + createdTimestamp + "}";
	}
}
