package com.dumbhippo.persistence;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class YahooArtistResult extends DBUnique {
	
	private static final long serialVersionUID = 1L;
	
	private long lastUpdated;
	private String artist;
	private String artistId;
	private String yahooMusicPageUrl;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	private boolean noResultsMarker;
	private int totalAlbumsByArtist;
	private boolean initialAlbumsStored;
	private boolean allAlbumsStored;
	
	
	public YahooArtistResult() {
		noResultsMarker = false;
		totalAlbumsByArtist = -1;
		initialAlbumsStored = false;
		allAlbumsStored = false;
	}

	public void update(YahooArtistResult results) {
		if (results.lastUpdated < lastUpdated)
			throw new RuntimeException("Updating artist with older results");
		
		lastUpdated = results.lastUpdated;
		artistId = results.artistId;
		artist = results.artist;
		yahooMusicPageUrl = results.yahooMusicPageUrl;
		smallImageUrl = results.smallImageUrl;
		smallImageWidth = results.smallImageWidth;
		smallImageHeight = results.smallImageHeight;
		noResultsMarker = results.noResultsMarker;
		totalAlbumsByArtist = results.totalAlbumsByArtist;
		initialAlbumsStored = results.initialAlbumsStored;
		allAlbumsStored = results.allAlbumsStored;
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

	/**
	 * For each Yahoo web services request, we can get back multiple 
	 * YahooArtistResult. If we get back 0, then we save one as a 
	 * marker that we got no results. If an artist has no rows in the db,
	 * that means we haven't ever done the web services request. However,
	 * if there is one row marked with the no results marker, it means
	 * that we did't find an artist with a given name.
	 * 
	 * @return whether this row marks that we did the request and got nothing
	 */
	@Column(nullable=false)
	public boolean isNoResultsMarker() {
		return this.noResultsMarker;
	}
	
	public void setNoResultsMarker(boolean noResultsMarker) {
		this.noResultsMarker = noResultsMarker;
	}

	@Column(nullable=false)
	public int getTotalAlbumsByArtist() {
		return totalAlbumsByArtist;
	}
	public void setTotalAlbumsByArtist(int totalAlbumsByArtist) {
		this.totalAlbumsByArtist = totalAlbumsByArtist;
	}
	
	@Column(nullable=false)
	public boolean isInitialAlbumsStored() {
	    return this.initialAlbumsStored;
	}
	
	public void setInitialAlbumsStored(boolean initialAlbumsStored) {
	    this.initialAlbumsStored = initialAlbumsStored;
	}
	
	@Column(nullable=false)
	public boolean isAllAlbumsStored() {
	    return this.allAlbumsStored;
	}
	
	public void setAllAlbumsStored(boolean allAlbumsStored) {
	    this.allAlbumsStored = allAlbumsStored;
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{YahooArtistResult:NoResultsMarker}";
		else
			return "{artist=" + artist + " artistId=" + artistId + "}";
	}
}
