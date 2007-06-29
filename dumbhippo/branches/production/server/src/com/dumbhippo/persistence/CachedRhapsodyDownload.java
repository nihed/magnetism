package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * A cached "RhapLink", or Rhapsody song download location, result.
 * 
 * See http://rhapsody.com/webservices for more information.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"url"})})
public class CachedRhapsodyDownload extends DBUnique implements CachedItem {
	private static final long serialVersionUID = 1L;

	private String url;
	private boolean active;
	private long lastUpdated;

	public CachedRhapsodyDownload() {
		super();
		lastUpdated = -1;
	}

	static public CachedRhapsodyDownload newNoResultsMarker() {
		return new CachedRhapsodyDownload();
	}
	
	// This is a little bogus; for the Rhapsody download web request, we treat 
	// "no results" and "false" as the same.
	@Transient
	public boolean isNoResultsMarker() {
		return !active;
	}
	
	@Column(nullable=false)
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Column(nullable=false)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Column(nullable=false)
	public Date getLastUpdated() {
		if (lastUpdated >= 0)
			return new Date(lastUpdated);
		else
			return null;
	}

	public void setLastUpdated(Date lastUpdated) {
		if (lastUpdated != null)
			this.lastUpdated = lastUpdated.getTime();
		else
			this.lastUpdated = -1;
	}
	
	@Override
	public String toString() {
		return "{CachedRhapsodyDownload active=" + active + " url=" + url + "}";
	}
}
