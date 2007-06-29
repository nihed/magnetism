package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.Thumbnail;
import com.dumbhippo.services.PicasaAlbum;
import com.dumbhippo.services.PicasaWebServices;

/** 
 * Cached Picasa web album from the public web album feed.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"owner", "url"})})
public class CachedPicasaAlbum extends DBUnique implements CachedListItem {
	private String url;	
	private String owner;
	private String title;
	private String thumbnail;	
	private long lastUpdated;
	
	private static final int MAX_PICASA_URL_LENGTH = 160;
	
	// for hibernate
	protected CachedPicasaAlbum() {
		if (MAX_PICASA_URL_LENGTH + PicasaWebServices.MAX_GOOGLE_USERNAME_LENGTH > 255)
			throw new RuntimeException("unique key on CachedPicasaAlbum will be too long");
	}
	
	public CachedPicasaAlbum(String owner, String title, String url, String thumbnail) {
		this.owner = owner;
		this.title = title;
		this.url = url;
		this.thumbnail = thumbnail;
	}
	
	static public CachedPicasaAlbum newNoResultsMarker(String username) {
		return new CachedPicasaAlbum(username, "", "", "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return url.length() == 0;
	}
	
	public CachedPicasaAlbum(String ownerId, Thumbnail result) {
		this(ownerId, result.getThumbnailTitle(), result.getThumbnailHref(), result.getThumbnailSrc()); 
	}
	
	public PicasaAlbum toThumbnail() {
		PicasaAlbum album = new PicasaAlbum(title, url, thumbnail);
		return album;
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedPicasaAlbum:NoResultsMarker}";
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

	@Column(nullable=false, length=PicasaWebServices.MAX_GOOGLE_USERNAME_LENGTH)
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

	@Column(nullable=false, length=MAX_PICASA_URL_LENGTH)	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
