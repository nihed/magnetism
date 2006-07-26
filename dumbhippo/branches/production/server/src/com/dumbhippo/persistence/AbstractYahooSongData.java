package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddableSuperclass;

import com.dumbhippo.services.YahooSongData;

/**
 * We have two tables, one for songs that are found for album listings, one for 
 * songs that are found for artist,album,name. This superclass has the common 
 * bits between those tables (the returned fields from yahoo) while the 
 * subclasses have the distinct keys used for lookup.
 */
@EmbeddableSuperclass
abstract public class AbstractYahooSongData extends DBUnique {
	
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
	
	public AbstractYahooSongData() {
		duration = -1;
		trackNumber = -1;
	}

	public void updateData(YahooSongData data) {
		// if albumId is our search key, then it's essential
		// that it matches the albumId set here ...
		albumId = data.getAlbumId();
		
		name = data.getName();		
		songId = data.getSongId();
		artistId = data.getArtistId();
		publisher = data.getPublisher();
		duration = data.getDuration();
		releaseDate = data.getReleaseDate();
		trackNumber = data.getTrackNumber();
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
	 * AbstractYahooSongData. If we get back 0, then we save one as a 
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

	@Column(nullable=true)
	public String getSongId() {
		return songId;
	}

	public void setSongId(String songId) {
		this.songId = songId;
	}

	@Column(nullable=true, length=100)
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
			return "{AbstractYahooSongData:NoResultsMarker}";
		else
			return "{songId=" + songId + " albumId=" + albumId + " artistId=" + artistId + "}";
	}
	
	private class Data implements YahooSongData {

		@Override
		public String toString() {
			return "{AbstractYahooSongData.SongData albumId=" + albumId + " songId=" + songId + " trackNumber=" + trackNumber + " name='" + name + "'}";
		}
		
		public String getAlbumId() {
			return albumId;
		}

		public String getArtistId() {
			return artistId;
		}

		public int getDuration() {
			return duration;
		}

		public String getPublisher() {
			return publisher;
		}

		public String getReleaseDate() {
			return releaseDate;
		}

		public String getSongId() {
			return songId;
		}

		public String getName() {
			return name;
		}

		public int getTrackNumber() {
			return trackNumber;
		}
		
	}
	
	public YahooSongData toData() {
		return new Data();
	}
}
