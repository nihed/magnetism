package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** 
 * Records persistent state of Last.Fm polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class LastFmUpdateStatus extends DBUnique {

	private String username;
	private String songHash;
	
	LastFmUpdateStatus() {
	}
	
	public LastFmUpdateStatus(String username) {
		this.username = username;
		songHash = "";
	}
	
	@Column(nullable=false,unique=true)
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(nullable=false)
	public String getSongHash() {
		return songHash;
	}
	public void setSongHash(String videoHash) {
		this.songHash = videoHash;
	}
}
