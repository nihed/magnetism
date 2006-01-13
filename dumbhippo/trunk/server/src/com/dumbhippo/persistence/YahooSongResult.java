package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="YahooSongResult", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"track_id","songId"})}
	      )
public class YahooSongResult extends DBUnique {
	
	private static final long serialVersionUID = 1L;

	private Track track;
	private long lastUpdated;
	private String songId;
	private String albumId;
	private String artistId;
	private String publisher;
	private int duration;
	private String releaseDate;
	private int trackNumber;
	
	public YahooSongResult() {
		
	}

	public void update(YahooSongResult results) {
		if (results.lastUpdated < lastUpdated)
			throw new RuntimeException("Updating song with older results");
		
		lastUpdated = results.lastUpdated;
		songId = results.songId;
		albumId = results.albumId;
		artistId = results.artistId;
		publisher = results.publisher;
		duration = results.duration;
		releaseDate = results.releaseDate;
		trackNumber = results.trackNumber;
	}
	
	// each track can have a couple of different song IDs that 
	// are interesting, in particular yahoo returns 
	// a different song id for iTunes and everything else
	@ManyToOne
	@JoinColumn(nullable=false)
	public Track getTrack() {
		return track;
	}

	public void setTrack(Track track) {
		this.track = track;
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

	@Column(nullable=false)
	public int getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}


}
