package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class YahooSongResult extends DBUnique {
	
	private static final long serialVersionUID = 1L;

	private long lastUpdated;
	private String songId;
	private String name;
	private String albumId;
	private String artistId;
	private String publisher;
	private int duration;
	private String releaseDate;
	private int trackNumber; // -1 is our default value for a track number, in case we will 
	                         // not obtain this information from yahoo request, though it is
	                         // usually available, 0 is the value yahoo assigns to tracks for 
	                         // which track number is inapplicable or unknown, valid tracks 
	                         // are 1-based
	private boolean noResultsMarker;
	
	public YahooSongResult() {
		duration = -1;
		trackNumber = -1;
	}

	public void update(YahooSongResult results) {
		if (results.lastUpdated < lastUpdated)
			throw new RuntimeException("Updating song with older results");
		
		lastUpdated = results.lastUpdated;
		songId = results.songId;
		name = results.name;
		albumId = results.albumId;
		artistId = results.artistId;
		publisher = results.publisher;
		duration = results.duration;
		releaseDate = results.releaseDate;
		trackNumber = results.trackNumber;
		noResultsMarker = results.noResultsMarker;
	}
	
	@Column(nullable=true)
	public String getAlbumId() {
		return albumId;
	}

	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}

	@Column(nullable=true)
	public String getArtistId() {
		return artistId;
	}

	public void setArtistId(String artistId) {
		this.artistId = artistId;
	}

	@Column(nullable=false)
	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}

	/**
	 * For each Yahoo web services request, we can get back multiple 
	 * YahooSongResult. If we get back 0, then we save one as a 
	 * marker that we got no results. If a song has no rows in the db,
	 * that means we haven't ever done the web services request.
	 * @return whether this row marks that we did the request and got nothing
	 */
	@Column(nullable=false)
	public boolean isNoResultsMarker() {
		return this.noResultsMarker;
	}
	
	public void setNoResultsMarker(boolean noResultsMarker) {
		this.noResultsMarker = noResultsMarker;
	}
	
	@Column(nullable=true)
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	@Column(nullable=true)
	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	@Column(nullable=true, unique=true)
	public String getSongId() {
		return songId;
	}

	public void setSongId(String songId) {
		this.songId = songId;
	}

	@Column(nullable=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Column(nullable=false)
	public int getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}

	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{YahooSongResult:NoResultsMarker}";
		else
			return "{songId=" + songId + " albumId=" + albumId + " artistId=" + artistId + "}";
	}
	
	@Transient
	public boolean isValid() {
	    if ((songId != null) && !songId.equals("") 
	        && (artistId != null) && !artistId.equals("") 
	        && (albumId != null) && !albumId.equals("")) {
	    	return true;
	    }
	    return false;
	}
}
