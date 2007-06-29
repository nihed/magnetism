package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.FlickrWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"flickrId"})})
public class CachedFlickrPhotos extends DBUnique implements CachedItem {

	private static final long serialVersionUID = 1L;
	
	private String flickrId;
	private int totalCount;
	private long lastUpdated;
	
	protected CachedFlickrPhotos() {
		
	}
	
	public CachedFlickrPhotos(String flickrId, int totalCount) {
		this.flickrId = flickrId;
		this.totalCount = totalCount;
	}
	
	public CachedFlickrPhotos(String flickrId, FlickrPhotosView view) {
		this.flickrId = flickrId;
		update(view);
	}
	
	static public CachedFlickrPhotos newNoResultsMarker(String flickrId) {
		return new CachedFlickrPhotos(flickrId, -1);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalCount < 0;
	}	

	public void update(FlickrPhotosView photos) {
		if (photos == null)
			totalCount = -1; // no results marker
		else
			setTotalCount(photos.getTotal());
	}
	
	public FlickrPhotosView toPhotos() {
		FlickrPhotos photos = new FlickrPhotos();
		photos.setPage(1); // flickr paging starts at 1
		photos.setPages(1);
		photos.setPerPage(totalCount);
		photos.setTotal(totalCount);
		return photos;
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
