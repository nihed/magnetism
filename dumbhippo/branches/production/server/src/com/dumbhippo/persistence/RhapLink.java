package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A cached "RhapLink", or Rhapsody song download location, result.
 * 
 * See http://rhapsody.com/webservices for more information.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"url"})})
public class RhapLink extends DBUnique {
	private static final long serialVersionUID = 1L;

	private String url;
	private boolean active;
	private long lastUpdated;

	public RhapLink() {
		super();
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
	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
