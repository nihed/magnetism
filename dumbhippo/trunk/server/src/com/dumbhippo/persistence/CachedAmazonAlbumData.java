package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.AmazonAlbumData;

@Entity
@Table(name="CachedAmazonAlbumData", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"artist","album"})}
	      )
public class CachedAmazonAlbumData extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private long lastUpdated;

	private String album;
	private String artist;
	
	private String ASIN;
	private String productUrl;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	public CachedAmazonAlbumData() {
		lastUpdated = -1;
		updateData(null);
	}
	
	public CachedAmazonAlbumData(String artist, String album, AmazonAlbumData data) {
		lastUpdated = -1;
		
		if (artist == null)
			throw new IllegalArgumentException("can't create CachedAmazonAlbumData with null artist");
		if (album == null)
			throw new IllegalArgumentException("can't create CachedAmazonAlbumData with null album");
		
		this.artist = artist;
		this.album = album;
		updateData(data);
	}

	public void updateData(AmazonAlbumData data) {
		if (data != null) {
			ASIN = data.getASIN();
			productUrl = data.getProductUrl();
			smallImageUrl = data.getSmallImageUrl();
			smallImageWidth = data.getSmallImageWidth();
			smallImageHeight = data.getSmallImageHeight();
		} else {
			ASIN = null;
			productUrl = null;
			smallImageUrl = null;
			smallImageWidth = -1;
			smallImageHeight = -1;
		}
	}
		
	// length of the whole unique key (album+artist) has to be 
	// under 1000 bytes with mysql, in UTF-8 encoding; mysql 
	// multiplies this char length by 4, so 4*100 + 4*100 = 800 bytes
	// if you use the hibernate default of 255 chars you get well over
	// 1000 bytes
	
	@Column(nullable=false,length=100)
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	// see comment on getAlbum() about the length
	@Column(nullable=false,length=100)
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	@Column(nullable=true)
	public String getASIN() {
		return ASIN;
	}
	public void setASIN(String asin) {
		ASIN = asin;
	}
	@Column(nullable=false)
	public Date getLastUpdated() {
		if (lastUpdated < 0)
			return null;
		else
			return new Date(lastUpdated);
	}
	public void setLastUpdated(Date lastUpdated) {
		if (lastUpdated == null)
			this.lastUpdated = -1;
		else
			this.lastUpdated = lastUpdated.getTime();
	}
	@Column(nullable=true)
	public String getProductUrl() {
		return productUrl;
	}
	public void setProductUrl(String productUrl) {
		this.productUrl = productUrl;
	}
	@Column(nullable=false)
	public int getSmallImageHeight() {
		return smallImageHeight;
	}
	public void setSmallImageHeight(int smallImageHeight) {
		this.smallImageHeight = smallImageHeight;
	}
	@Column(nullable=true)
	public String getSmallImageUrl() {
		return smallImageUrl;
	}
	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}
	@Column(nullable=false)
	public int getSmallImageWidth() {
		return smallImageWidth;
	}
	public void setSmallImageWidth(int smallImageWidth) {
		this.smallImageWidth = smallImageWidth;
	}
	
	@Override
	public String toString() {
		return "{albumResult album=" + album + " artist=" + artist + " imageUrl=" + smallImageUrl + "}";
	}

	/** 
	 * Implementing AmazonAlbumData in the outer class
	 * would leak an implementation detail and 
	 * remove some type safety.
	 * 
	 * This trick is slightly shady since changing the 
	 * album result will also change the album data,
	 * it would be mildly more kosher
	 * to make this a static inner class with its own
	 * data fields I suppose. But since CachedAmazonAlbumData 
	 * is only used inside AmazonAlbumCacheBean it's not a 
	 * big deal exactly.
	 */
	private class Data implements AmazonAlbumData {

		public String getASIN() {
			return ASIN;
		}

		public String getProductUrl() {
			return productUrl;
		}

		public String getSmallImageUrl() {
			return smallImageUrl;
		}

		public int getSmallImageWidth() {
			return smallImageWidth;
		}

		public int getSmallImageHeight() {
			return smallImageHeight;
		}

		public String getAlbum() {
			return album;
		}

		public String getArtist() {
			return artist;
		}
	}
	
	public AmazonAlbumData toData() {
		return new Data();
	}	
}
