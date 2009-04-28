package com.dumbhippo.persistence.caches;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.persistence.DBUnique;
import com.dumbhippo.services.smugmug.rest.bind.Image;

/** 
 * Cached Smugmug web album from the public web album feed.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"thumbUrl", "owner"})})
public class CachedSmugmugAlbum extends DBUnique implements CachedListItem {
	private String imageId;
	private String imageKey;
	private String caption;
	private Date lastUpdated;
	private String thumbUrl;
	private String owner;
	  
	// for hibernate
	protected CachedSmugmugAlbum() 
	{
	}
	
	public CachedSmugmugAlbum(String imageId, String imageKey, 
			String caption, Date lastUpdated, String thumbUrl, String owner) 
	{
		this.imageId = imageId;
		this.imageKey = imageKey;
		this.caption = caption;
		this.thumbUrl = thumbUrl;
		this.owner = owner;
	}
	
	static public CachedSmugmugAlbum newNoResultsMarker(String owner) {
		return new CachedSmugmugAlbum("", "", "", new Date(), "", owner);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return thumbUrl.length() == 0;
	}
	
	private static Date parseDate(String value)
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date result = new Date();
		try
		{
			result = format.parse(value);
		}
		catch(ParseException e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public CachedSmugmugAlbum(String owner, Image result) {
		this(result.getId(), result.getKey(), result.getCaption(),
				parseDate(result.getLastUpdated()), result.getThumbURL(), owner); 
	}
	
	public Image toImage() {
		Image image = new Image();
		image.setId(imageId);
		image.setKey(imageKey);
		image.setCaption(caption);
		image.setLastUpdated(lastUpdated.toString());
		image.setThumbURL(thumbUrl);
		return image;
	}
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedSmugmugAlbum:NoResultsMarker}";
		else
			return String.format("{imageId=%1$s imageKey=%2$s caption=%3$s lastUpdated=%4$s thumbUrl=%5$s owner=%6$s}", 
					new Object[]{imageId, imageKey, caption, lastUpdated, thumbUrl, owner});
	}

	@Column(nullable=false)
	public Date getLastUpdated() 
	{
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Column(nullable=false)
	public String getThumbUrl() {
		return thumbUrl;
	}

	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}
	
	@Column(nullable=false)
	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	@Column(nullable=false)
	public String getImageKey() {
		return imageKey;
	}

	public void setImageKey(String imageKey) {
		this.imageKey = imageKey;
	}
	
	@Column(nullable=true)	
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	@Column(nullable=false)	
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
}
