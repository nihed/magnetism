package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.YouTubeWebServices;

/** 
 * Cached fields from YouTube videos feed.
 * 
 * @author Colin Walters
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"owner", "url"})})
public class CachedYouTubeVideo extends DBUnique implements CachedListItem {
	private String url;	
	private String owner;
	private String title;
	private String thumbnail;	
	private long lastUpdated;
	
	private static final int MAX_YOUTUBE_URL_LENGTH = 128;
	
	// for hibernate
	protected CachedYouTubeVideo() {
	}
	
	public CachedYouTubeVideo(String owner, String title, String url, String thumbnail) {
		this.owner = owner;
		this.title = title;
		this.url = url;
		this.thumbnail = thumbnail;
	}
	
	static public CachedYouTubeVideo newNoResultsMarker(String username) {
		return new CachedYouTubeVideo(username, "", "", "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return url.length() == 0;
	}
	
	public CachedYouTubeVideo(String ownerId, Thumbnail result) {
		this(ownerId, result.getThumbnailTitle(), result.getThumbnailHref(), result.getThumbnailSrc()); 
	}
	
	public YouTubeVideo toThumbnail() {
		YouTubeVideo video = new YouTubeVideo(title, url, thumbnail);
		return video;
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedYouTubeVideo:NoResultsMarker}";
		else
			return "{ownerId=" + owner + " url=" + url + " title='" + title + "'}";
	}

	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}

	@Column(nullable=false, length=YouTubeWebServices.MAX_YOUTUBE_USERNAME_LENGTH)
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Column(nullable=false)
	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	@Column(nullable=false)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Column(nullable=false, length=MAX_YOUTUBE_URL_LENGTH)	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
