package com.dumbhippo.server;

import java.util.EnumMap;
import java.util.Map;

import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;

public class TrackView {
	 
	private String artist;
	private String album;
	private String name;
	private Map<SongDownloadSource,String> downloads;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	public TrackView() {
		// a "dummy" track view with no data in it
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
	}
	
	public TrackView(Track track) {
		this();
		this.artist = track.getArtist();
		this.album = track.getAlbum();
		this.name = track.getName();
	}

	public String getDownloadUrl(SongDownloadSource source) {
		if (downloads == null)
			return null;
		else
			return downloads.get(source);
	}
	
	public void setDownloadUrl(SongDownloadSource source, String url) {
		if (downloads == null)
			downloads = new EnumMap<SongDownloadSource,String>(SongDownloadSource.class);
		downloads.put(source, url);
	}
	
	// These getDownloadUrl wrappers are convenient from JSTL expression language
	
	public String getYahooUrl() {
		return getDownloadUrl(SongDownloadSource.YAHOO);
	}

	public String getItunesUrl() {
		return getDownloadUrl(SongDownloadSource.ITUNES);
	}

	public String getRhapsodyUrl() {
		return getDownloadUrl(SongDownloadSource.RHAPSODY);
	}
	
	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
}
