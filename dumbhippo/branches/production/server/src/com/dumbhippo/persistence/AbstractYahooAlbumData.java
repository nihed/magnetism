package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.dumbhippo.services.YahooAlbumData;

@MappedSuperclass
abstract public class AbstractYahooAlbumData extends DBUnique {
	
	private static final long serialVersionUID = 1L;
	
	private long lastUpdated;
	private String albumId;
	private String artistId;	
	private String album;
	private String artist;
	private String publisher;
	private String releaseDate;
	private int tracksNumber;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	private void reset() {
		albumId = null;
		artistId = null;
		album = null;
		artist = null;
		publisher = null;
		releaseDate = null;
		tracksNumber = -1;
		smallImageUrl = null;
		smallImageWidth = -1;
		smallImageHeight = -1;
	}
	
	public AbstractYahooAlbumData() {
		reset();
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return album == null;
	}	
	
	protected void updateData(String albumId, String artistId, YahooAlbumData data) {
		if (albumId == null && artistId == null)
			throw new IllegalArgumentException("must set either album id or artist id so the cache has a key");
		if (data != null) {
			if (albumId != null && !albumId.equals(data.getAlbumId()))
				throw new IllegalArgumentException("albumId passed to updateData() must match albumId in the data");
			if (artistId != null && !artistId.equals(data.getArtistId()))
				throw new IllegalArgumentException("artistId passed to updateData() must match artistId in the data");
			
			this.albumId = data.getAlbumId();
			this.artistId = data.getArtistId();
			album = data.getAlbum();
			artist = data.getArtist();
			publisher = data.getPublisher();
			releaseDate = data.getReleaseDate();
			tracksNumber = data.getTracksNumber();
			smallImageUrl = data.getSmallImageUrl();
			smallImageWidth = data.getSmallImageWidth();
			smallImageHeight = data.getSmallImageHeight();
		} else {
			reset();
			this.albumId = albumId;
			this.artistId = artistId;
		}		
	}
	
	// actual getAlbumId is in the subclass since its annotations vary by subclass
	final protected String internalGetAlbumId() {
		return albumId;
	}

	final protected void internalSetAlbumId(String albumId) {
		this.albumId = albumId;
	}
	
	// actual getArtistId is in the subclass since its annotations vary by subclass
	final protected String internalGetArtistId() {
		return artistId;
	}

	final protected void internalSetArtistId(String artistId) {
		this.artistId = artistId;
	}
	
	@Column(nullable=true)
	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	@Column(nullable=true)
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	@Column(nullable=true)
	public String getSmallImageUrl() {
		return smallImageUrl;
	}
	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}
	
	@Column(nullable=false)
	public int getSmallImageHeight() {
		return smallImageHeight;
	}
	public void setSmallImageHeight(int smallImageHeight) {
		this.smallImageHeight = smallImageHeight;
	}

	@Column(nullable=false)
	public int getSmallImageWidth() {
		return smallImageWidth;
	}
	public void setSmallImageWidth(int smallImageWidth) {
		this.smallImageWidth = smallImageWidth;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
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

	@Column(nullable=false)
	public int getTracksNumber() {
		return tracksNumber;
	}

	public void setTracksNumber(int tracksNumber) {
		this.tracksNumber = tracksNumber;
	}
		
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{AbstractYahooAlbumData:NoResultsMarker}";
		else
			return "{artistId=" + artistId + " albumId=" + albumId + " tracksNumber=" + tracksNumber + "}";
	}
	
	private class Data implements YahooAlbumData {

		@Override
		public String toString() {
			return "{AbstractYahooAlbumData.Data albumId=" + getAlbumId() + " artistId=" + getArtistId() + " album='" + getAlbum() + "'}";
		}		
		
		public String getAlbumId() {
			return albumId;
		}

		public String getAlbum() {
			return album;
		}

		public String getArtistId() {
			return artistId;
		}

		public String getArtist() {
			return artist;
		}

		public String getPublisher() {
			return publisher;
		}

		public String getReleaseDate() {
			return releaseDate;
		}

		public int getTracksNumber() {
			return tracksNumber;
		}

		public String getSmallImageUrl() {
			return smallImageUrl;
		}

		public int getSmallImageWidth() {
			return smallImageWidth;
		}

		public int getSmallImageHeight() {
			return smallImageHeight;
		}
	}
	
	public YahooAlbumData toData() {
		return new Data();
	}
}
