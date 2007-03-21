package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** 
 * Records persistent state of Picasa polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class PicasaUpdateStatus extends DBUnique {

	private String username;
	private String albumHash;
	
	PicasaUpdateStatus() {
	}
	
	public PicasaUpdateStatus(String username) {
		this.username = username;
		albumHash = "";
	}
	
	@Column(nullable=false,unique=true)
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(nullable=false)
	public String getAlbumHash() {
		return albumHash;
	}
	public void setAlbumHash(String albumHash) {
		this.albumHash = albumHash;
	}
}
