package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

public class AlbumView {

	private String title;
	private ArtistView artistView;

	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	private List<TrackView> tracks;

	public AlbumView() {
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.artistView = new ArtistView();
		tracks = new ArrayList<TrackView>();
	}

	public AlbumView(String album, String artist) {
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.artistView = new ArtistView(artist);
		this.title = album;
		tracks = new ArrayList<TrackView>();
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
		return artistView.getName();
	}

	public void setArtist(String artist) {
		artistView.setName(artist);
	}
	
	public ArtistView getArtistView() {
		return artistView;
	}
	
	public List<TrackView> getTracks() {
		return tracks;
	}
	
	public void addTrack(TrackView track) {
		tracks.add(track);
	}

	public void setTracks(List<TrackView> tracks) {
		this.tracks = tracks;
	}
}
