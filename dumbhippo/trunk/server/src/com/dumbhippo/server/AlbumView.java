package com.dumbhippo.server;

import com.dumbhippo.persistence.Track;

public class AlbumView {

	private String title;
	private String artist;

	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;

	public AlbumView() {
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
	}

	public AlbumView(Track track) {
		this();
		this.title = track.getAlbum();
		this.artist = track.getArtist();
	}
	
	public int getSmallImageHeight() {
		return smallImageHeight;
	}

	public void setSmallImageHeight(int smallImageHeight) {
		this.smallImageHeight = smallImageHeight;
	}

	public String getSmallImageUrl() {
		return smallImageUrl;
	}

	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}

	public int getSmallImageWidth() {
		return smallImageWidth;
	}

	public void setSmallImageWidth(int smallImageWidth) {
		this.smallImageWidth = smallImageWidth;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}
}
