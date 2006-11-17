package com.dumbhippo.services;

import java.util.Date;

public class FacebookPhotoData implements FacebookPhotoDataView {

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
	
	@Override
	public String toString() {
		return "{FacebookPhotoData photoId=" + photoId + " link=" + link + " source=" + source +
		" caption=" + caption + " createdTimestamp" + createdTimestamp + "}";
	}
}
