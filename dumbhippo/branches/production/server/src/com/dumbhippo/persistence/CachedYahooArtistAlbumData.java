package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

import com.dumbhippo.services.YahooAlbumData;

@Entity
@org.hibernate.annotations.Table(appliesTo = "CachedYahooArtistAlbumData",
		indexes = { 
			@Index(name="artistId_index", columnNames = { "artistId", "id" }),
			@Index(name="artistAlbum_index", columnNames = { "artist", "album", "id" })
		})
@javax.persistence.Table(name="CachedYahooArtistAlbumData",
		uniqueConstraints = {
			@UniqueConstraint(columnNames={"artistId","albumId"})
		})
public class CachedYahooArtistAlbumData
	extends AbstractYahooAlbumData implements CachedListItem {

	// Observationally,  the maximum length we've seen for Yahoo ID's is 35
	private static final int MAX_YAHOO_ID_LENGTH = 64;
	
	private static final long serialVersionUID = 1L;

	static public CachedYahooArtistAlbumData newNoResultsMarker(String artistId) {
		CachedYahooArtistAlbumData d = new CachedYahooArtistAlbumData();
		d.setArtistId(artistId);
		return d;
	}
	
	public void updateData(String artistId, YahooAlbumData data) {
		updateData(null, artistId, data);
	}

	// this probably could be unique on its own because if an album belongs to multiple 
	// artists yahoo has a distinct artist id for each combination of 
	// artists, i.e. the artist is "Joe and Bob" not "Joe" and "Bob" - thus there's 
	// no expected duplicate albumId.
	// However the global unique constraint on albumId,artistId covers it 
	// anyhow.
	// This is nullable for when we're storing a negative result.
	@Column(nullable=true, unique=false, length=MAX_YAHOO_ID_LENGTH)
	public String getAlbumId() {
		return internalGetAlbumId();
	}

	public void setAlbumId(String albumId) {
		internalSetAlbumId(albumId);
	}
	
	// this isn't unique on its own, since we're storing a list of albums for an artist
	@Column(nullable=false, unique=false, length=MAX_YAHOO_ID_LENGTH)
	public String getArtistId() {
		return internalGetArtistId();
	}

	public void setArtistId(String artistId) {
		internalSetArtistId(artistId);
	}
}
