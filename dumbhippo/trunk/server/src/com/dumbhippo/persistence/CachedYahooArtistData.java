package com.dumbhippo.persistence;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.services.YahooArtistData;

@Entity
public class CachedYahooArtistData extends DBUnique {
	
	private static final long serialVersionUID = 1L;
	
	private long lastUpdated;
	private String artist;
	private String artistId;
	private String yahooMusicPageUrl;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	public CachedYahooArtistData() {
	}

	public void updateData(YahooArtistData data) {
		if (data != null) {
			artistId = data.getArtistId();
			artist = data.getArtist();
			yahooMusicPageUrl = data.getYahooMusicPageUrl();
			smallImageUrl = data.getSmallImageUrl();
			smallImageWidth = data.getSmallImageWidth();
			smallImageHeight = data.getSmallImageHeight();
		} else {
			artistId = null;
			artist = null;
			yahooMusicPageUrl = null;
			smallImageUrl = null;
			smallImageWidth = -1;
			smallImageHeight = -1;
		}
	}
	
	@Column(nullable=true, unique = true)
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
	public String getYahooMusicPageUrl() {
		return yahooMusicPageUrl;
	}
	public void setYahooMusicPageUrl(String yahooMusicPageUrl) {
		this.yahooMusicPageUrl = yahooMusicPageUrl;
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
		return this.artist == null;
	}
	
	@Override
	public String toString() {
		return "{artist=" + artist + " artistId=" + artistId + "}";
	}
	
	private class Data implements YahooArtistData {

		public String getArtist() {
			return artist;
		}

		public String getArtistId() {
			return artistId;
		}

		public int getSmallImageHeight() {
			return smallImageHeight;
		}

		public String getSmallImageUrl() {
			return smallImageUrl;
		}

		public int getSmallImageWidth() {
			return smallImageWidth;
		}

		public String getYahooMusicPageUrl() {
			return yahooMusicPageUrl;
		}
	}
	
	public YahooArtistData toData() {
		return new Data();
	}
}
