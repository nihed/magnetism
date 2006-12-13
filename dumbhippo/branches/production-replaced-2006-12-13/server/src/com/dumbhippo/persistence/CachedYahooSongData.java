package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;

/**
 * This table maps (album,artist,name) triplet to yahoo song data.
 * The album,artist,name are the search keys, not anything returned by
 * Yahoo!, in fact the same yahoo song ID could be under multiple search key triplets.
 */
@Entity
@org.hibernate.annotations.Table(appliesTo = "CachedYahooSongData", indexes={ 
		@Index(name="albumArtistSong_index", columnNames = { "searchedAlbum", "searchedArtist", "searchedName", "id" } ) 
})
public class CachedYahooSongData extends AbstractYahooSongData {
	private static final long serialVersionUID = 1L;

	private String searchedArtist;
	private String searchedAlbum;
	private String searchedName;
	
	@Column(nullable=true, length=100)
	public String getSearchedAlbum() {
		return searchedAlbum;
	}
	public void setSearchedAlbum(String album) {
		this.searchedAlbum = album;
	}
	
	@Column(nullable=true, length=100)
	public String getSearchedArtist() {
		return searchedArtist;
	}
	public void setSearchedArtist(String artist) {
		this.searchedArtist = artist;
	}
	
	@Column(nullable=true, length=100)
	public String getSearchedName() {
		return searchedName;
	}
	public void setSearchedName(String searchedName) {
		this.searchedName = searchedName;
	}
}
