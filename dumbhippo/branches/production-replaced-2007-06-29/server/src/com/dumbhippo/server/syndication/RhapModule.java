package com.dumbhippo.server.syndication;

import com.sun.syndication.feed.CopyFrom;
import com.sun.syndication.feed.module.Module;

/**
 * Rhapsody RSS module for Rome.
 */
public interface RhapModule extends Module,CopyFrom {
	
	/**
	 * URI of the Rhapsody module; Note that Rhapsody uses rhap for
	 * both the prefix and URI and defines the namespace at the 
	 * element level, not document level.  E.g.:
	 * <pre>
	 *   <rhap:artist xmlns:rhap="rhap">Sleater-Kinney</rhap:artist>
	 * </pre>
	 */
	public static final String URI = "rhap";
	public static final String PREFIX = "rhap";
	
	public String getArtist();
	public String getArtistRCID();
	public String getAlbum();
	public String getAlbumRCID();
	public String getTrack();
	public String getTrackRCID();
	public String getDuration();
	public String getPlayHref();
	public String getDataHref();
	public String getAlbumArt();
	
	public void setArtist(String artist);
	public void setArtistRCID(String artistRCID);
	public void setAlbum(String album);
	public void setAlbumRCID(String albumRCID);
	public void setTrack(String track);
	public void setTrackRCID(String trackRCID);
	public void setDuration(String duration);
	public void setPlayHref(String playHref);
	public void setDataHref(String dataHref);
	public void setAlbumArt(String albumArt);
}
