package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** 
 * Records persistent state of YouTube polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class YouTubeUpdateStatus extends DBUnique {

	private String username;
	private String videoHash;
	
	YouTubeUpdateStatus() {
	}
	
	public YouTubeUpdateStatus(String username) {
		this.username = username;
		videoHash = "";
	}
	
	@Column(nullable=false,unique=true)
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(nullable=false)
	public String getVideoHash() {
		return videoHash;
	}
	public void setVideoHash(String videoHash) {
		this.videoHash = videoHash;
	}
}
