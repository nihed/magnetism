package com.dumbhippo.persistence;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Table(name = "YahooAlbumResult", indexes={ 
		@Index(name="artistAlbum_index", columnNames = { "artist", "album", "id" } ) 
})
public class YahooAlbumResult extends DBUnique {
	
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
	private boolean noResultsMarker;
	private boolean allSongsStored;
	
	public YahooAlbumResult() {
		noResultsMarker = false;
		allSongsStored = false;
	}

	public void update(YahooAlbumResult results) {
		if (results.lastUpdated < lastUpdated)
			throw new RuntimeException("Updating album with older results");
		
		lastUpdated = results.lastUpdated;
		albumId = results.albumId;
		album = results.album;
		artistId = results.artistId;
		artist = results.artist;
		publisher = results.publisher;
		releaseDate = results.releaseDate;
		tracksNumber = results.tracksNumber;
		smallImageUrl = results.smallImageUrl;
		smallImageWidth = results.smallImageWidth;
		smallImageHeight = results.smallImageHeight;
		noResultsMarker = results.noResultsMarker;
		allSongsStored = results.allSongsStored;
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
	
	@Column(nullable=false)
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

	/**
	 * For each Yahoo web services request, we can get back multiple 
	 * YahooAlbumResult. If we get back 0, then we save one as a 
	 * marker that we got no results. If an artist has no rows in the db,
	 * that means we haven't ever done the web services request. However,
	 * if there is one row marked with the no results marker, it means
	 * that the artist has no albums.
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
	public boolean isAllSongsStored() {
	    return this.allSongsStored;
	}
	
	public void setAllSongsStored(boolean allSongsStored) {
	    this.allSongsStored = allSongsStored;
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
	
	@Transient
	public int getReleaseYear() {
		if (releaseDate == null)
			return -1;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd");
		try {
		    Date date = dateFormat.parse(releaseDate);
		    Calendar calendar = new GregorianCalendar();
		    calendar.setTime(date);
		    return calendar.get(Calendar.YEAR);
		} catch (ParseException e) {
			return -1;
		}		
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{YahooAlbumResult:NoResultsMarker}";
		else
			return "{artistId=" + artistId + " albumId=" + albumId + " tracksNumber=" + tracksNumber + "}";
	}
}
