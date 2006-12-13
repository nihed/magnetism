package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class FacebookAlbumData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private FacebookAccount facebookAccount;
	private FacebookEvent facebookEvent;
    private FacebookPhotoData coverPhoto;
    String albumId;
    String name;
	long createdTimestamp;
	long modifiedTimestamp;
	String description;
	String location;
	
	public FacebookAlbumData() {
		this.coverPhoto = new FacebookPhotoData();
	}
	
	public FacebookAlbumData(FacebookAccount facebookAccount, FacebookPhotoData coverPhoto, String albumId,
			                 String name, long createdTimestamp, long modifiedTimestamp, String description,
			                 String location) {
        this.facebookAccount = facebookAccount;
        this.coverPhoto = coverPhoto;
        this.albumId = albumId;
        this.name = name;
        this.createdTimestamp = createdTimestamp;
        this.modifiedTimestamp = modifiedTimestamp;
        this.description = description;
        this.location = location;
	}
	
	public void updateCachedData(FacebookAlbumData albumData) {
		// this should not update facebookAccount or facebookEvent
		coverPhoto.updateCachedData(albumData.getCoverPhoto());
		albumId = albumData.getAlbumId();
		name = albumData.getName();
	    createdTimestamp = albumData.getCreatedTimestampAsLong();
	    modifiedTimestamp = albumData.getModifiedTimestampAsLong();
	    description = albumData.getDescription();
	    location = albumData.getLocation();
	}
	
	@ManyToOne
	@JoinColumn(nullable = false)
	public FacebookAccount getFacebookAccount() {
		return facebookAccount;
	}
	
	public void setFacebookAccount(FacebookAccount facebookAccount) {
		this.facebookAccount = facebookAccount;
	}

	// we want this to be one to one, so that when an album is modified,
	// we can modify the timestamp on the event and reuse it, rather than list 
	// a new one
	// FacebookEvent is nullable because the albums that we first get when the 
	// user adds their facebook account, do not have facebook events associated 
	// with them
	@OneToOne(optional=true)
	@JoinColumn(nullable=true, unique=true)
	public FacebookEvent getFacebookEvent() {
		return facebookEvent;
	}
	
	public void setFacebookEvent(FacebookEvent facebookEvent) {
		this.facebookEvent = facebookEvent;
	}
	
	@OneToOne
	@JoinColumn(nullable=false, unique=true)
	public FacebookPhotoData getCoverPhoto() {
		return coverPhoto;
	}
	
	public void setCoverPhoto(FacebookPhotoData coverPhoto) {
		this.coverPhoto = coverPhoto;
	}
	
	@Column(nullable=false)
	public String getAlbumId() {
		return albumId;
	}
	
	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}	
	
	@Column(nullable=false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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
	public Date getModifiedTimestamp() {
		return new Date(modifiedTimestamp);
	}

	@Transient
	public long getModifiedTimestampAsLong() {
		return modifiedTimestamp;
	}
	
	public void setModifiedTimestamp(Date modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp.getTime();
	}
	
	public void setModifiedTimestampAsLong(long modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp;
	}
	
	@Column(nullable=false)
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}	
	
	@Column(nullable=false)
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}	
}
