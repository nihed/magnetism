package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.StringUtils;

public class AlbumView {

	private String title;
	private ArtistView artistView;
	private int releaseYear;
	
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	private String productUrl;
	
	private List<TrackView> tracks;

	public AlbumView() {
		this.releaseYear = -1;
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.artistView = new ArtistView();
		tracks = new ArrayList<TrackView>();
	}

	public AlbumView(String album, String artist) {
		this.releaseYear = -1;
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.artistView = new ArtistView(artist);
		this.title = album;
		tracks = new ArrayList<TrackView>();
	}
	
	public int getSmallImageHeight() {
		if (smallImageUrl == null)
			return 75;
		
		return smallImageHeight;
	}

	public void setSmallImageHeight(int smallImageHeight) {
		this.smallImageHeight = smallImageHeight;
	}

	public String getSmallImageUrl() {
		if (smallImageUrl == null)
			return "/images/no_image_available75x75light.gif";

		return smallImageUrl;
	}
	
	public boolean isSmallImageUrlAvailable() {
		if (smallImageUrl == null)
			return false;
		
		return true;
	}

	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}

	public int getSmallImageWidth() {
		if (smallImageUrl == null)
			return 75;
		
		return smallImageWidth;
	}

	public void setSmallImageWidth(int smallImageWidth) {
		this.smallImageWidth = smallImageWidth;
	}

	public String getProductUrl() {
		return productUrl;
	}
	
	public void setProductUrl(String productUrl) {
		this.productUrl = productUrl;
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
	
	public int getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(int releaseYear) {
		this.releaseYear = releaseYear;
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
	
	public int getNumberOfTracks() {
		return tracks.size();
	}
	
	public String getTruncatedTitle() {
	    return StringUtils.truncateString(title, 31);	
	}
}
