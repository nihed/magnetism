package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhoto;
import com.dumbhippo.services.FlickrPhotoView;

/** 
 * Cached results of the Flickr "get photos for user" call
 * 
 * @author Havoc Pennington
 */
@Entity
// I think the flickrId for photos may be globally unique not per-user so this is paranoia
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"ownerId","flickrId"})})
public class CachedFlickrUserPhoto extends DBUnique implements CachedListItem {
	private String ownerId;
	private String flickrId;
	private String server;
	private String secret;
	private String title;
	private long lastUpdated;
	
	// for hibernate
	protected CachedFlickrUserPhoto() {
		
	}
	
	public CachedFlickrUserPhoto(String ownerId, String flickrId, String server, String secret, String title) {
		this.ownerId = ownerId;
		this.flickrId = flickrId;		
		this.server = server;
		this.secret = secret;
		this.title = title;
	}
	
	static public CachedFlickrUserPhoto newNoResultsMarker(String ownerId) {
		return new CachedFlickrUserPhoto(ownerId, "", "", "", "");
	}	
	
	@Transient
	public boolean isNoResultsMarker() {
		return flickrId.length() == 0;
	}
	
	public CachedFlickrUserPhoto(String ownerId, FlickrPhotoView result) {
		this(ownerId, result.getId(), result.getServer(), result.getSecret(), result.getTitle()); 
	}
	
	public FlickrPhotoView toPhoto() {
		FlickrPhoto photo = new FlickrPhoto();
		photo.setId(flickrId);
		photo.setServer(server);
		photo.setSecret(secret);
		photo.setTitle(title);
		return photo;
	}
	
	@Column(nullable=false)
	public String getFlickrId() {
		return flickrId;
	}
	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}
	
	@Column(nullable=false)
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	
	@Column(nullable=false)
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	@Column(nullable=false)
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	
	@Column(nullable=false)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}	
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedFlickrUserPhoto:NoResultsMarker}";
		else
			return "{ownerId=" + ownerId + " flickrId=" + flickrId + " title='" + title + "'}";
	}
}
