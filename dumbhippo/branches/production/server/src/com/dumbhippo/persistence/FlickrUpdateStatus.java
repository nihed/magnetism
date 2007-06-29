package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.services.FlickrWebServices;

/** 
 * This is our persistent state for Flickr polling. i.e. we check whether there's new 
 * stuff on Flickr periodically and this table stores what stuff we had at the last 
 * check, so we can see if anything changed.
 * 
 * This info comes from web services, but is NOT just a cache, unlike the 
 * Cached* tables. If you drop this table people will get new block timestamps.
 * 
 * One FlickrUpdateStatus can be shared among multiple Mugshot accounts without 
 * a problem, since it's just keyed by Flickr ID.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class FlickrUpdateStatus extends DBUnique {

	private String flickrId;
	 
	private String mostRecentPhotos;
	private String mostRecentPhotosets;
	
	FlickrUpdateStatus() {
		mostRecentPhotos = "";
		mostRecentPhotosets = "";
	}
	
	public FlickrUpdateStatus(String flickrId) {
		this();
		this.flickrId = flickrId;
	}
	
	/** flickr user ID this is the status for. */
	@Column(nullable=false, unique=true, length=FlickrWebServices.MAX_FLICKR_USER_ID_LENGTH)
	public String getFlickrId() {
		return flickrId;
	}
	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}
	
	/** 
	 * Set to a concatenation of the most recent photo IDs, which 
	 * works as a "hash" of current photo state so we can know if 
	 * it changed.
	 */
	@Column(nullable=false)
	public String getMostRecentPhotos() {
		return mostRecentPhotos;
	}
	public void setMostRecentPhotos(String mostRecentPhotos) {
		this.mostRecentPhotos = mostRecentPhotos;
	}
	
	/** 
	 * Set to a concatenation of the most recent photoset IDs, which 
	 * works as a "hash" of current photoset state so we can know if 
	 * it changed.
	 */
	@Column(nullable=false)
	public String getMostRecentPhotosets() {
		return mostRecentPhotosets;
	}
	public void setMostRecentPhotosets(String mostRecentPhotosets) {
		this.mostRecentPhotosets = mostRecentPhotosets;
	}
}
