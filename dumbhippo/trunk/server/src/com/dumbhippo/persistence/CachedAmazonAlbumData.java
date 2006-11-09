package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.caches.AlbumAndArtist;

@Entity
@Table(name="CachedAmazonAlbumData", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"artist","album"})}
	      )
public class CachedAmazonAlbumData extends DBUnique {

	private static final long serialVersionUID = 1L;

	public static final int DATA_COLUMN_LENGTH = 100;
	
	private long lastUpdated;

	private String album;
	private String artist;
	
	private String ASIN;
	private String productUrlPart1;
	private String productUrlPart2;
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

	public CachedAmazonAlbumData(AlbumAndArtist albumAndArtist, AmazonAlbumData data) {
		this(albumAndArtist.getArtist(), albumAndArtist.getAlbum(), data);
	}

	public void updateData(AmazonAlbumData data) {
		if (data != null) {
			ASIN = data.getASIN();
			setProductUrl(data.getProductUrl());
			smallImageUrl = data.getSmallImageUrl();
			smallImageWidth = data.getSmallImageWidth();
			smallImageHeight = data.getSmallImageHeight();
		} else {
			ASIN = null;
			setProductUrl(null);
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
	
	@Column(nullable=false,length=DATA_COLUMN_LENGTH)
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	// see comment on getAlbum() about the length
	@Column(nullable=false,length=DATA_COLUMN_LENGTH)
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
	public String getProductUrlPart1() {
		return productUrlPart1;
	}
	public void setProductUrlPart1(String productUrlPart1) {
		this.productUrlPart1 = productUrlPart1;
	}
	@Column(nullable=true)
	public String getProductUrlPart2() {
		return productUrlPart2;
	}
	public void setProductUrlPart2(String productUrlPart2) {
		this.productUrlPart2 = productUrlPart2;
	}
	@Transient
	public String getProductUrl() {
		String part1 = getProductUrlPart1();
		String part2 = getProductUrlPart2();
		if (part2 != null) {
			return part1+part2;
		} else {
			return part1;
		}
	}
	public void setProductUrl(String productUrl) {
		if (productUrl == null) {
			setProductUrlPart1(null);
			setProductUrlPart2(null);
			return;
		}
		if (productUrl.length()<=255) {
			setProductUrlPart1(productUrl);
			setProductUrlPart2(null);
		} else {
			String part1 = productUrl.substring(0, 255);
			String part2 = productUrl.substring(255);
			setProductUrlPart1(part1);
			setProductUrlPart2(part2);
		}
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

		@Override
		public String toString() {
			return "{CachedAmazonAlbumData.Data ASIN=" + ASIN + " productUrl=" + productUrlPart1+productUrlPart2 + " album='" + album + "'}";
		}		
		
		public String getASIN() {
			return ASIN;
		}

		public String getProductUrl() {
			String part1 = getProductUrlPart1();
			String part2 = getProductUrlPart2();
			if (part2 != null) {
				return part1+part2;
			} else {
				return part1;
			}
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
