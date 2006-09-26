package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class FacebookEvent extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private FacebookEventType eventType;
	private FacebookAccount facebookAccount;
	private int count;  
	private long eventTimestamp;
	private FacebookAlbumData album;
	private Set<FacebookPhotoData> photos;
	
	protected FacebookEvent() {
		photos = new HashSet<FacebookPhotoData>();
	}
	
	public FacebookEvent(FacebookAccount facebookAccount, FacebookEventType eventType, int count, long eventTimestamp) {
		this();
	    this.facebookAccount = facebookAccount;
	    this.eventType = eventType;
	    this.count = count;
	    this.eventTimestamp = eventTimestamp;
	}
	
	@ManyToOne
	@JoinColumn(nullable = false)
	public FacebookAccount getFacebookAccount() {
		return facebookAccount;
	}
	
	public void setFacebookAccount(FacebookAccount facebookAccount) {
		this.facebookAccount = facebookAccount;
	}

	@Column(nullable=false)
	public FacebookEventType getEventType() {
		return eventType;
	}

	public void setEventType(FacebookEventType eventType) {
		this.eventType = eventType;
	}
	
	@Column(nullable=false)
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Column(nullable=false)
	public Date getEventTimestamp() {
		return new Date(eventTimestamp);
	}

	@Transient
	public long getEventTimestampAsLong() {
		return eventTimestamp;
	}
	
	public void setEventTimestamp(Date eventTimestamp) {
		this.eventTimestamp = eventTimestamp.getTime();
	}
	
	public void setEventTimestampAsLong(long eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	// optional=true is the default value for OneToOne, 
	// it means that the relationship can be null
	@OneToOne(optional=true, mappedBy="facebookEvent")
	public FacebookAlbumData getAlbum() {
		return album;
	}

	public void setAlbum(FacebookAlbumData album) {
		this.album = album;
	}
	
	// this is one to many because we currently store multiple
	// FacebookPhotoData if different people are tagged in the same photo
	@OneToMany(mappedBy="facebookEvent")
	public Set<FacebookPhotoData> getPhotos() {
		return photos;
	}
	
	public void setPhotos(Set<FacebookPhotoData> photos) {
		if (photos == null)
			throw new IllegalArgumentException("null photos");
		this.photos = photos;
	}
	
	public void addPhoto(FacebookPhotoData photo) {
		photos.add(photo);
	}
	
	public void removePhoto(FacebookPhotoData photo) {
		photos.remove(photo);
	}
}
