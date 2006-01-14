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
	
	public TrackView() {
		// a "dummy" track view with no data in it
	}
	
	public TrackView(Track track) {
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
}
