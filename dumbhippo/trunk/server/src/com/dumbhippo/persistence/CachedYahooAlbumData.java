package com.dumbhippo.persistence;

import javax.persistence.Entity;

import org.hibernate.annotations.Index;

@Entity 
@org.hibernate.annotations.Table(name = "CachedYahooAlbumData", indexes={ 
		@Index(name="artistAlbum_index", columnNames = { "artist", "album", "id" } ) 
})
public class CachedYahooAlbumData extends YahooAlbumResult {
	private static final long serialVersionUID = 1L;

}
