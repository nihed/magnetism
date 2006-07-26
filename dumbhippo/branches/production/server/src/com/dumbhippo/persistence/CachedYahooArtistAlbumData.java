package com.dumbhippo.persistence;

import javax.persistence.Entity;

import org.hibernate.annotations.Index;

@Entity
@org.hibernate.annotations.Table(name = "CachedYahooArtistAlbumData", indexes={ 
		@Index(name="artistId_index", columnNames = { "artistId", "id" }),
		@Index(name="artistAlbum_index", columnNames = { "artist", "album", "id" }) 
})
public class CachedYahooArtistAlbumData extends AbstractYahooAlbumData {

	private static final long serialVersionUID = 1L;

}
