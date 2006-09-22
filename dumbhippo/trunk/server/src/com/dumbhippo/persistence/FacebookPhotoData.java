package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class FacebookPhotoData extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private FacebookAccount facebookAccount;
	private FacebookEvent facebookEvent;
	String link;
	String source;
	String caption;
	long createdTimestamp;
	// could later be a FacebookAlbumData
	String albumId;
	
	public FacebookPhotoData() {}
	
	public FacebookPhotoData(FacebookAccount facebookAccount, String link, String source, String caption, long createdTimestamp, String albumId) {
        this.facebookAccount = facebookAccount;
		this.link = link;
        this.source = source;
        this.caption = caption;
        this.createdTimestamp = createdTimestamp;
        this.albumId = albumId;
	}
	
	public void updateCachedData(FacebookPhotoData photoData) {
		// this should not update facebookAccount or facebookEvent
	    link = photoData.getLink();
	    source = photoData.getSource();
	    caption = photoData.getCaption();
	    createdTimestamp = photoData.getCreatedTimestampAsLong();
	    albumId = photoData.getAlbumId();
	}
	
	@ManyToOne
	@JoinColumn(nullable = false)
	public FacebookAccount getFacebookAccount() {
		return facebookAccount;
	}
	
	public void setFacebookAccount(FacebookAccount facebookAccount) {
		this.facebookAccount = facebookAccount;
	}

	@ManyToOne
	@JoinColumn(nullable = true)
	public FacebookEvent getFacebookEvent() {
		return facebookEvent;
	}
	
	public void setFacebookEvent(FacebookEvent facebookEvent) {
		this.facebookEvent = facebookEvent;
	}
	
	@Column(nullable=false)
	public String getLink() {
		return link;
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
}
