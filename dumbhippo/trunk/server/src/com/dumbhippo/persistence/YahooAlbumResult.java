package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddableSuperclass;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

import com.dumbhippo.services.YahooAlbumData;

@EmbeddableSuperclass
abstract public class YahooAlbumResult extends DBUnique {
	
	private static final long serialVersionUID = 1L;
	
	private long lastUpdated;
	private String albumId;
	private String album;
	private String artistId;
	private String artist;
	private String publisher;
	private String releaseDate;
	private int tracksNumber;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	public YahooAlbumResult() {
		updateData(null);
	}

	public void updateData(YahooAlbumData data) {
		if (data != null) {
			albumId = data.getAlbumId();
			album = data.getAlbum();
			artistId = data.getArtistId();
			artist = data.getArtist();
			publisher = data.getPublisher();
			releaseDate = data.getReleaseDate();
			tracksNumber = data.getTracksNumber();
			smallImageUrl = data.getSmallImageUrl();
			smallImageWidth = data.getSmallImageWidth();
			smallImageHeight = data.getSmallImageHeight();
		} else {
			albumId = null;
			album = null;
			artistId = null;
			artist = null;
			publisher = null;
			releaseDate = null;
			tracksNumber = -1;
			smallImageUrl = null;
			smallImageWidth = -1;
			smallImageHeight = -1;
		}
	}
	
	// Yahoo album results always have a single artist element.
    // If an album has multiple artists, then we get that back as
    // as a single result with an artist like 
    // "B.B. King & Eric Clapton" rather than multiple results.
	// Every such combination of artists has its own id. 
	// Thus, we would not get multiple YahooAlbumResult entries
	// when an album has multiple artists, so albumId is unique 
	// by itself and doesn't need to be combined with artistId to 
	// provide a unique key.
	@JoinColumn(nullable=true, unique=true)
	public String getAlbumId() {
		return albumId;
	}

	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}

	@Column(nullable=true)
	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}
	
	@Column(nullable=true)
	public String getArtistId() {
		return artistId;
	}

	public void setArtistId(String artistId) {
		this.artistId = artistId;
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

	@Transient
	public boolean isNoResultsMarker() {
		return album == null;
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
			return "{YahooAlbumResult:NoResultsMarker}";
		else
			return "{artistId=" + artistId + " albumId=" + albumId + " tracksNumber=" + tracksNumber + "}";
	}
	
	private class Data implements YahooAlbumData {

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
