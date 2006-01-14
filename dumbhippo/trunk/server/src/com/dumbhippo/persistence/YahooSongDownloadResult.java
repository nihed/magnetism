package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="YahooSongDownloadResult", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"songId","source"})}
	      )
public class YahooSongDownloadResult extends DBUnique {
	private static final long serialVersionUID = 1L;

	private String songId;
	private long lastUpdated;
	private SongDownloadSource source;
	private String url;
	private String price;
	private String restrictions;
	private String formats;

	public YahooSongDownloadResult() {
		
	}

	public void update(YahooSongDownloadResult results) {
		if (!songId.equals(results.songId) ||
				!(source == results.source))
			throw new RuntimeException("Updating song download with wrong result");
		if (results.lastUpdated < lastUpdated)
			throw new RuntimeException("Updating song download with older results");
		
		lastUpdated = results.lastUpdated;
		price = results.price;
		restrictions = results.restrictions;
		formats = results.formats;
		url = results.url;
	}

	@Column(nullable=false)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(nullable=true)
	public String getFormats() {
		return formats;
	}

	public void setFormats(String format) {
		this.formats = format;
	}

	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}

	@Column(nullable=true)
	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	@Column(nullable=true)
	public String getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(String restrictions) {
		this.restrictions = restrictions;
	}

	@Column(nullable=false)
	public String getSongId() {
		return songId;
	}

	public void setSongId(String songId) {
		this.songId = songId;
	}

	@Column(nullable=false)
	public SongDownloadSource getSource() {
		return source;
	}

	public void setSource(SongDownloadSource source) {
		this.source = source;
	}
	
	@Override
	public String toString() {
		return "{songId=" + songId + " source=" + source + " url=" + url + "}";
	}
}
