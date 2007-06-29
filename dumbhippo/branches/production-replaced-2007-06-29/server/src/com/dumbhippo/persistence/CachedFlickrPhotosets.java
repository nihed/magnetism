package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhotosets;
import com.dumbhippo.services.FlickrPhotosetsView;
import com.dumbhippo.services.FlickrWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"flickrId"})})
public class CachedFlickrPhotosets extends DBUnique implements CachedItem {

	private static final long serialVersionUID = 1L;
	
	private String flickrId;
	private int totalCount;
	private long lastUpdated;
	
	protected CachedFlickrPhotosets() {
		
	}
	
	public CachedFlickrPhotosets(String flickrId, int totalCount) {
		this.flickrId = flickrId;
		this.totalCount = totalCount;
	}
	
	public CachedFlickrPhotosets(String flickrId, FlickrPhotosetsView view) {
		this.flickrId = flickrId;
		update(view);
	}
	
	static public CachedFlickrPhotosets newNoResultsMarker(String flickrId) {
		return new CachedFlickrPhotosets(flickrId, -1);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalCount < 0;
	}	

	public void update(FlickrPhotosetsView photos) {
		if (photos == null)
			totalCount = -1; // no results marker
		else
			setTotalCount(photos.getTotal());
	}
	
	public FlickrPhotosetsView toPhotosets() {
		FlickrPhotosets photosets = new FlickrPhotosets();
		photosets.setTotal(totalCount);
		return photosets;
	}
	
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_USER_ID_LENGTH)
	public String getFlickrId() {
		return flickrId;
	}
	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}
	
	@Column(nullable=false)
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
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
			return "{flickrId=" + flickrId + "}";
	}
}
