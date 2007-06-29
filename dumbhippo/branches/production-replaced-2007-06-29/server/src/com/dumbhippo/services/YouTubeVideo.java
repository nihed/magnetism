package com.dumbhippo.services;

import com.dumbhippo.Thumbnail;


public final class YouTubeVideo implements Thumbnail {
	private static final int YOUTUBE_THUMBNAIL_WIDTH = 120;
	private static final int YOUTUBE_THUMBNAIL_HEIGHT = 90;
	private String title;
	private String src;
	private String thumbnail;
	
	public YouTubeVideo(String title, String src, String thumbnail) {
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
		return YOUTUBE_THUMBNAIL_HEIGHT;
	}	
	
	public int getThumbnailWidth() {
		return YOUTUBE_THUMBNAIL_WIDTH;
	}
}
