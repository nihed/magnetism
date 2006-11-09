package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhotoset;
import com.dumbhippo.services.FlickrPhotosetView;

/** 
 * Cached results of the Flickr "get photosets for user" call
 * 
 * @author Havoc Pennington
 */
@Entity
// I think the flickrId for photosets may be globally unique not per-user so this is paranoia
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"ownerId","flickrId"})})
public class CachedFlickrUserPhotoset extends DBUnique implements CachedListItem {
	private String ownerId;
	private String flickrId;
	private String title;
	private String description;
	private int photoCount;
	private long lastUpdated;

	// for hibernate
	protected CachedFlickrUserPhotoset() {
		
	}
	
	public CachedFlickrUserPhotoset(String ownerId, String flickrId, String title, String description, int photoCount) {
		this.ownerId = ownerId;
		this.flickrId = flickrId;
		this.title = title;
		this.description = description;
		this.photoCount = photoCount;
	}	
	
	static public CachedFlickrUserPhotoset newNoResultsMarker(String ownerId) {
		return new CachedFlickrUserPhotoset(ownerId, "", "", "", 0);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return flickrId.length() == 0;
	}
	
	public CachedFlickrUserPhotoset(String ownerId, FlickrPhotosetView photoset) {
		this(ownerId, photoset.getId(), photoset.getTitle(), photoset.getDescription(), photoset.getPhotoCount());
	}
	
	public FlickrPhotosetView toPhotoset() {
		FlickrPhotoset photoset = new FlickrPhotoset();
		photoset.setId(flickrId);
		photoset.setTitle(title);
		photoset.setDescription(description);
		photoset.setPhotoCount(photoCount);
		return photoset;
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
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(nullable=false)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(nullable=false)
	public int getPhotoCount() {
		return photoCount;
	}

	public void setPhotoCount(int photoCount) {
		this.photoCount = photoCount;
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
			return "{CachedFlickrUserPhotoset:NoResultsMarker}";
		else
			return "{ownerId=" + ownerId + " flickrId=" + flickrId + " title='" + title + "'}";
	}	
}
