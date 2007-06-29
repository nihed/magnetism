package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhoto;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrWebServices;

/** 
 * Cached results of the Flickr "get photos for user" call
 * 
 * @author Havoc Pennington
 */
@Entity
// I think the flickrId for photos may be globally unique not per-set/user so this is paranoia
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"setId","flickrId"})})
public class CachedFlickrPhotosetPhoto extends DBUnique implements CachedListItem {
	private String setId;
	private String flickrId;
	private String server;
	private String secret;
	private String title;
	private long lastUpdated;
	
	// for hibernate
	protected CachedFlickrPhotosetPhoto() {
		
	}
	
	public CachedFlickrPhotosetPhoto(String setId, String flickrId, String server, String secret, String title) {
		this.setId = setId;
		this.flickrId = flickrId;		
		this.server = server;
		this.secret = secret;
		this.title = title;
	}

	static public CachedFlickrPhotosetPhoto newNoResultsMarker(String setId) {
		return new CachedFlickrPhotosetPhoto(setId, "", "", "", "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return flickrId.length() == 0;
	}	
	
	public CachedFlickrPhotosetPhoto(String setId, FlickrPhotoView result) {
		this(setId, result.getId(), result.getServer(), result.getSecret(), result.getTitle()); 
	}
	
	public FlickrPhotoView toPhoto() {
		FlickrPhoto photo = new FlickrPhoto();
		photo.setId(flickrId);
		photo.setServer(server);
		photo.setSecret(secret);
		photo.setTitle(title);
		return photo;
	}
	
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_PHOTO_ID_LENGTH)
	public String getFlickrId() {
		return flickrId;
	}
	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}
	
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_PHOTOSET_ID_LENGTH)
	public String getSetId() {
		return setId;
	}
	public void setSetId(String setId) {
		this.setId = setId;
	}
	
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_SECRET_LENGTH)
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_SERVER_LENGTH)
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
			return "{CachedFlickrPhotosetPhoto:NoResultsMarker}";
		else
			return "{setId=" + setId + " flickrId=" + flickrId + " title='" + title + "'}";
	}
}
