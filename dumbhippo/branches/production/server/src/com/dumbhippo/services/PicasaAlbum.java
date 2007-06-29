package com.dumbhippo.services;

import com.dumbhippo.Thumbnail;


public final class PicasaAlbum implements Thumbnail {
	private static final int PICASA_THUMBNAIL_WIDTH = 160;
	private static final int PICASA_THUMBNAIL_HEIGHT =160;
	private String title;
	private String src;
	private String thumbnail;
	
	public PicasaAlbum(String title, String src, String thumbnail) {
		this.title = title;
		this.src = src;
		this.thumbnail = thumbnail;
	}
	
	public String getThumbnailHref() {
		return src;
	}
	
	public String getThumbnailSrc() {
		return thumbnail;
	}
	public String getThumbnailTitle() {
		return title;
	}

	public int getThumbnailHeight() {
		return PICASA_THUMBNAIL_HEIGHT;
	}	
	
	public int getThumbnailWidth() {
		return PICASA_THUMBNAIL_WIDTH;
	}
}
