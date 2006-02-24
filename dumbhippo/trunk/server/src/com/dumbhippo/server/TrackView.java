package com.dumbhippo.server;

import java.util.EnumMap;
import java.util.Map;

import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;

/**
 * FIXME the AlbumView is kind of retrofitted in here, it should 
 * really be TrackView.getAlbumView() and drop most of the "wrapper"
 * accessors
 * 
 * @author hp
 *
 */
public class TrackView {
	 
	private AlbumView album;
	
	private String name;
	private Map<SongDownloadSource,String> downloads; 
	private int durationSeconds;
	private long lastListenTime;
	
	public TrackView() {
		album = new AlbumView();
	}
	
	public TrackView(Track track) {
		this.album = new AlbumView(track);
		this.name = track.getName();
		this.durationSeconds = track.getDuration();
	}

	public AlbumView getAlbumView() {
		return album;
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
		return album.getTitle();
	}

	public void setAlbum(String album) {
		this.album.setTitle(album);
	}

	public String getArtist() {
		return album.getArtist();
	}

	public void setArtist(String artist) {
		album.setArtist(artist);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSmallImageHeight() {
		return album.getSmallImageHeight();
	}

	public void setSmallImageHeight(int smallImageHeight) {
		album.setSmallImageHeight(smallImageHeight);
	}

	public String getSmallImageUrl() {
		return album.getSmallImageUrl();
	}

	public void setSmallImageUrl(String smallImageUrl) {
		album.setSmallImageUrl(smallImageUrl);
	}

	public int getSmallImageWidth() {
		return album.getSmallImageWidth();
	}

	public void setSmallImageWidth(int smallImageWidth) {
		album.setSmallImageWidth(smallImageWidth);
	}

	public int getDurationSeconds() {
		return durationSeconds;
	}
	
	public void setDurationSeconds(int durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public long getLastListenTime() {
		return lastListenTime;
	}

	public void setLastListenTime(long lastListenTime) {
		this.lastListenTime = lastListenTime;
	}	
	
	@Override
	public String toString() {
		return "{trackView artist=" + getArtist() + " album=" + getAlbum() + " name=" + getName() + "}";
	}

}
