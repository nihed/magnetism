package com.dumbhippo.persistence;

import javax.persistence.Entity;

/**
 * Subclass of FeedEntry representing a track list feed (e.g., from Rhapsody's web services)
 */

@Entity
public class TrackFeedEntry extends FeedEntry {
	private static final long serialVersionUID = 1L;
	
	private String artist;
	private String artistRCID;
	private String album;
	private String albumRCID;
	private String track;
	private String trackRCID;
	private String duration;
	private String playHref;
	private String dataHref;
	private String albumArt;
	
	protected TrackFeedEntry() {
		super();
	}
	
	public TrackFeedEntry(Feed feed) {
		super(feed);
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbumArt() {
		return albumArt;
	}

	public void setAlbumArt(String albumArt) {
		this.albumArt = albumArt;
	}

	public String getAlbumRCID() {
		return albumRCID;
	}

	public void setAlbumRCID(String albumRCID) {
		this.albumRCID = albumRCID;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getArtistRCID() {
		return artistRCID;
	}

	public void setArtistRCID(String artistRCID) {
		this.artistRCID = artistRCID;
	}

	public String getDataHref() {
		return dataHref;
	}

	public void setDataHref(String dataHref) {
		this.dataHref = dataHref;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getPlayHref() {
		return playHref;
	}

	public void setPlayHref(String playHref) {
		this.playHref = playHref;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public String getTrackRCID() {
		return trackRCID;
	}

	public void setTrackRCID(String trackRCID) {
		this.trackRCID = trackRCID;
	}

	@Override
	public String toString() {
		return "{TrackFeedEntry feed = " + getFeed() + " title = " + getTitle() + " date = " + getDate() + "}";
	}
}
