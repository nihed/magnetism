package com.dumbhippo.server.syndication;

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * Rhapsody RSS module implementation for Rome.
 */
public class RhapModuleImpl extends ModuleImpl implements RhapModule {
	
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
	
	public RhapModuleImpl() {
		super(RhapModule.class, URI);
	}
	
	public final Class getInterface() {
		return RhapModule.class;
	}
	
	public final void copyFrom(Object obj) {
		RhapModule rm = (RhapModule)obj;
		setArtist(rm.getArtist());
		setArtistRCID(rm.getArtistRCID());
		setAlbum(rm.getAlbum());
		setAlbumRCID(rm.getAlbumRCID());
		setTrack(rm.getTrack());
		setTrackRCID(rm.getTrackRCID());
		setDuration(rm.getDuration());
		setPlayHref(rm.getPlayHref());
		setDataHref(rm.getDataHref());
		setAlbumArt(rm.getAlbumArt());
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
}
