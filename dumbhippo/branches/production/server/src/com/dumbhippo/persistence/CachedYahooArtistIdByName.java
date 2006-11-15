package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * This caches a mapping from artist name to artistId; there may be
 * multiple names for one id, e.g. "Belle and Sebastian" "Belle & Sebastian"
 *
 * In the CachedYahooArtistData table, we also have an artist name, in that case
 * the one that Yahoo! returned for a given artistId, instead of the one we 
 * used for the search.
 */
@Entity
@Table(name="CachedYahooArtistIdByName", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"name","artistId"})}
	      )
public class CachedYahooArtistIdByName extends DBUnique {

	private static final long serialVersionUID = 1L;

	public static final int DATA_COLUMN_LENGTH = 100;
	
	private String name;
	private String artistId;
	private long lastUpdated;
	
	public CachedYahooArtistIdByName() {
	}

	// null artistId means a cached negative name lookup
	@Column(nullable=true,length=DATA_COLUMN_LENGTH)
	public String getArtistId() {
		return artistId;
	}

	public void setArtistId(String artistId) {
		this.artistId = artistId;
	}

	@Column(nullable=false,length=DATA_COLUMN_LENGTH)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	 
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return artistId == null;
	}
	
	@Override
	public String toString() {
		return "{CachedYahooArtistIdByName artistId=" + artistId + " name='" + name + "'}";
	}
}
