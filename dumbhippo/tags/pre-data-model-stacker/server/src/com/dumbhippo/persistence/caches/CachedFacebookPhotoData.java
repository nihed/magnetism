package com.dumbhippo.persistence.caches;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.DBUnique;
import com.dumbhippo.services.FacebookPhotoData;
import com.dumbhippo.services.FacebookPhotoDataView;
import com.dumbhippo.services.FacebookWebServices;

@Entity

@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "facebookPhotoId"})})
public class CachedFacebookPhotoData extends DBUnique implements CachedListItem {
	// if multiple users have the same facebook account registered, we need to know whose 
	// session to use to update the cached data, so we should store the mugshot user 
	// id rather than the facebook api user id as the key here
	private String userId;
	private String link;
	private String source;
	private String caption;
	private long createdTimestamp;
	private String albumId;
	private long lastUpdated;
	private String facebookPhotoId;
	
	// for hibernate
	protected CachedFacebookPhotoData() {	
	}	
	
	public CachedFacebookPhotoData(String userId, String link, String source, String caption, long createdTimestamp, String albumId) {
        this.userId = userId;
		this.link = link;
        this.source = source;
        this.caption = caption;
        this.createdTimestamp = createdTimestamp;
        this.albumId = albumId;
        this.facebookPhotoId = getPhotoId();
	}
	
	public CachedFacebookPhotoData(String userId, FacebookPhotoDataView photoData) {
		this(userId, photoData.getLink(), photoData.getSource(), photoData.getCaption(), photoData.getCreatedTimestamp().getTime(), photoData.getAlbumId());
	}
	
	public FacebookPhotoDataView toPhotoData() {
		FacebookPhotoData photoData = new FacebookPhotoData();
		photoData.setLink(link);
		photoData.setSource(source);
		photoData.setCaption(caption);
		photoData.setCreatedTimestamp(new Date(createdTimestamp));
		photoData.setAlbumId(albumId);
		photoData.setPhotoId(facebookPhotoId);
		return photoData;
	}
	
	static public CachedFacebookPhotoData newNoResultsMarker(String userId) {
		return new CachedFacebookPhotoData(userId, "", "", "", -1, "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return link.length() == 0;
	}
	
	public void updateCachedData(CachedFacebookPhotoData photoData) {
	    link = photoData.getLink();
	    source = photoData.getSource();
	    caption = photoData.getCaption();
	    createdTimestamp = photoData.getCreatedTimestampAsLong();
	    albumId = photoData.getAlbumId();
	    facebookPhotoId = photoData.getFacebookPhotoId();
	}
	
	@Column(nullable=false, length=Guid.STRING_LENGTH)
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Column(nullable=false)
	public String getLink() {
		return link;
	}
	
	public void setFacebookPhotoId(String facebookPhotoId) {
		this.facebookPhotoId = facebookPhotoId;
	}	

	@Column(nullable=false, length=FacebookWebServices.MAX_FACEBOOK_PHOTO_ID_LENGTH)
	public String getFacebookPhotoId() {
		return facebookPhotoId;
	}
	
	public void setLink(String link) {
		this.link = link;
	}	
	
	@Column(nullable=false)
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}	
	
	@Column(nullable=false)
	public String getCaption() {
		return caption;
	}
	
	public void setCaption(String caption) {
		this.caption = caption;
	}	
	
	@Column(nullable=false)
	public Date getCreatedTimestamp() {
		return new Date(createdTimestamp);
	}

	@Transient
	public long getCreatedTimestampAsLong() {
		return createdTimestamp;
	}
	
	public void setCreatedTimestamp(Date createdTimestamp) {
		this.createdTimestamp = createdTimestamp.getTime();
	}
	
	public void setCreatedTimestampAsLong(long createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}
	
	@Column(nullable=false)
	public String getAlbumId() {
		return albumId;
	}
	
	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}	
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}	
	
	@Transient 
	public String getPhotoId() {
		return FacebookPhotoData.getPhotoIdFromLink(link);
	}
	
	@Transient
	public boolean isValid() {
		// for album cover photos, facebook returns some invalid link and source even if the
		// photo is not present, this check seems to be a good indicator of whether the photo
		// is valid
		return (getCreatedTimestampAsLong() > 0);
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedFacebookPhotoData:NoResultsMarker}";
		else
			return "{userId=" + userId + " facebookPhotoId=" + facebookPhotoId + " caption='" + caption + "'}";
	}	
}
