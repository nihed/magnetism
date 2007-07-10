package com.dumbhippo.persistence.caches;

import javax.persistence.Entity;

import org.hibernate.annotations.Index;


/**
 * This table maps albumId to Yahoo song results
 */
@Entity
@org.hibernate.annotations.Table(appliesTo = "CachedYahooAlbumSongData", indexes={ 
		@Index(name="albumId_index", columnNames = { "albumId", "id" })
})
public class CachedYahooAlbumSongData extends AbstractYahooSongData {
	private static final long serialVersionUID = 1L;

	static public CachedYahooAlbumSongData newNoResultsMarker(String albumId) {
		CachedYahooAlbumSongData d = new CachedYahooAlbumSongData();
		d.setAlbumId(albumId);
		d.setNoResultsMarker(true);
		return d;
	}
}
