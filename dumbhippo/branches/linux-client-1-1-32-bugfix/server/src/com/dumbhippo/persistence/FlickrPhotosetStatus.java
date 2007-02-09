package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrPhotoset;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrWebServices;

/** 
 * CachedFlickrUserPhotoset is a stored return from the web service, while this is a never-deleted 
 * Guid-having object that's the referent of a Flickr photoset block. Instead of ever 
 * deleting this, we just setActive(false).
 * 
 * FIXME this is broken that it stores the title and description; those things should be in a pure-cache
 * object returned from a cached web service session bean.
 * 
 * @author Havoc Pennington
 *
 */
@Entity
//I think the flickrId for photosets may be globally unique not per-user so this is paranoia
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"ownerId","flickrId"})})
public class FlickrPhotosetStatus extends EmbeddedGuidPersistable {
	private String ownerId;
	private String flickrId;
	private String title;
	private String description;
	private int photoCount;
	private boolean active;
	
	protected FlickrPhotosetStatus() {
		this.title = "";
		this.description = "";
	}
	
	public FlickrPhotosetStatus(String ownerId, String flickrId) {
		this();
		this.ownerId = ownerId;
		this.flickrId = flickrId;
	}
	
	// returns true if anything was modified
	public boolean update(FlickrPhotosetView view) {
		if (view == null) {
			if (this.active) {
				setActive(false);
				return true;
			} else {
				return false;
			}
		} else {
			if (!view.getId().equals(flickrId))
				throw new RuntimeException("updating FlickrPhotosetStatus with view " + view + " but status is " + this);
			
			boolean modified = false;
			
			if (!this.active) {
				setActive(true);
				modified = true;
			}
			if (!this.title.equals(view.getTitle())) {
				setTitle(view.getTitle());
				modified = true;
			}
			if (!this.description.equals(view.getDescription())) {
				setDescription(view.getDescription());
				modified = true;
			}
			if (this.photoCount != view.getPhotoCount()) {
				setPhotoCount(view.getPhotoCount());
				modified = true;
			}
			
			return modified;
		}
	}
	
	/** 
	 * FIXME this is busted; we should not be storing the title/description anyway, those 
	 * things should be returned from a cached web service, and all callers of this should 
	 * be changed to instead use said web service that would return a FlickrPhotosetView 
	 * given a photoset ID.
	 * @return
	 */
	public FlickrPhotosetView toPhotoset(FlickrPhotos photos) {
		FlickrPhotoset photoset = new FlickrPhotoset();
		photoset.setId(flickrId);		
		if (isActive()) {
			photoset.setTitle(title);
			photoset.setDescription(description);
			photoset.setPhotoCount(photoCount);
			photoset.setPhotos(photos);
		} else {
			photoset.setTitle("Not available");
			photoset.setDescription("Photoset has been deleted or is currently unavailable");
			photoset.setPhotoCount(0);
		}
		return photoset;
	}
	
	/** true if the photoset was in the list of the user's photosets last time we called
	 * the web service, which should also mean our fields are valid.
	 */
	@Column(nullable=false)
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	@Column(nullable=false)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_PHOTOSET_ID_LENGTH)
	public String getFlickrId() {
		return flickrId;
	}
	public void setFlickrId(String flickrId) {
		this.flickrId = flickrId;
	}
	@Column(nullable=false, length=FlickrWebServices.MAX_FLICKR_USER_ID_LENGTH)
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	@Column(nullable=false)
	public int getPhotoCount() {
		return photoCount;
	}
	public void setPhotoCount(int photoCount) {
		this.photoCount = photoCount;
	}
	@Column(nullable=false)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public String toString() {
		return "{guid=" + getId() + " ownerId=" + ownerId + " flickrId=" + flickrId + " title='" + title + "'}";
	}
}
