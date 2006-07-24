package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;

/**
 * This table maps (album,artist,name) triplet to yahoo song data.
 */
@Entity
@org.hibernate.annotations.Table(name = "CachedYahooSongData", indexes={ 
		@Index(name="albumArtistSong_index", columnNames = { "album", "artist", "name", "id" } ) 
})
public class CachedYahooSongData extends AbstractYahooSongData {
	private static final long serialVersionUID = 1L;

	private String artist;
	private String album;
	
	@Column(nullable=true, length=100)
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	
	@Column(nullable=true, length=100)
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
}
