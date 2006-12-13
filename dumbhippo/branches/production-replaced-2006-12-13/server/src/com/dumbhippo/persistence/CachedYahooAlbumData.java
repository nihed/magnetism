package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;

import com.dumbhippo.services.YahooAlbumData;

@Entity 
@org.hibernate.annotations.Table(appliesTo = "CachedYahooAlbumData", indexes={ 
		@Index(name="artistAlbum_index", columnNames = { "artist", "album", "id" } ) 
})
public class CachedYahooAlbumData extends AbstractYahooAlbumData {
	private static final long serialVersionUID = 1L;

	public void updateData(String albumId, YahooAlbumData data) {
		updateData(albumId, null, data);
	}
	
	@Column(nullable=false, unique=true)
	public String getAlbumId() {
		return internalGetAlbumId();
	}

	public void setAlbumId(String albumId) {
		internalSetAlbumId(albumId);
	}
	
	@Column(nullable=true, unique=false)
	public String getArtistId() {
		return internalGetArtistId();
	}

	public void setArtistId(String artistId) {
		internalSetArtistId(artistId);
	}
}
