package com.dumbhippo.persistence;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;

/**
 * The Track object is intended to be immutable once it's 
 * created in the database, and potentially shared among
 * multiple users. Thus, you shouldn't add properties to it
 * that belong in a per-user table, such as last time it was
 * listened to, etc.
 * 
 * The "setters" are all protected, because they are unsafe for 
 * use outside of the EJB3 persistence layer; they don't invalidate
 * the digest. The reason is that we would like to just use the 
 * digest from the database, rather than having to recompute it 
 * every time we load a track, and EJB3 will call the setters.
 * Clients of this class can only use the setProperties() method,
 * which does invalidate the digest.
 * 
 * If you add fields you must update both getDigest() and 
 * setProperties() to use them...
 * 
 * We might end up wanting to drop the digest and just lookup the 
 * track by querying for a match on all fields. But since I coded
 * it this way first, let's try it this way first. Maybe more work 
 * in the app server and less in the database is good, who knows.
 * 
 * If we remove fields, we may need to include serialVersionUID
 * in the hash, or leave an update(null) in the hash, to avoid a
 * situation like:
 * - digest.update(oldField);
 * + digest.update(newField);
 * where null for both fields would create the same digest. Adding
 * or removing fields effectively creates a new "generation" of tracks,
 * which is perhaps a downside of the digest approach.
 * 
 * @author hp
 *
 */
@Entity
@Table(name="Track", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"digest"})}
	      )
@Indexed(index="track")
// Needs to be serializable since it is used as a cluster-wide locking key
// when doing web service requests to look up track information
public class Track extends DBUnique implements Serializable {
	private static final long serialVersionUID = 1L;
	static private final Logger logger = GlobalSetup.getLogger(Track.class);
	
	private String digest;
	private TrackType type;
	private MediaFileFormat format;
	private String name;
	private String artist;
	private String album;
	private String url;
	private int duration; // length in seconds, -1 if unknown
	private long fileSize; // in bytes, -1 if unknown
	private int trackNumber; // -1 if inapplicable/unknown, 1-based, 0 is invalid
	private String discIdentifier; // CD/DVD ID string, aka MCDI, TOC

	private void computeDigest() {
		if (digest != null)
			throw new IllegalStateException("Track already has digest, now immutable");
		
		MessageDigest md = Digest.newDigest();
		Digest.update(md, getType().ordinal());
		Digest.update(md, getFormat().ordinal());
		Digest.update(md, getName());
		Digest.update(md, getArtist());
		Digest.update(md, getAlbum());
		Digest.update(md, getUrl());
		Digest.update(md, getDuration());
		Digest.update(md, getFileSize());
		Digest.update(md, getTrackNumber());
		Digest.update(md, getDiscIdentifier());
		setDigest(Digest.digest(md));		
	}
	
	/** 
	 * Content digest of this track.
	 * 
	 * @return the content hash of this track
	 */
	@Column(length = Digest.SHA1_HEX_LENGTH, nullable = false)
	public String getDigest() {
		return digest;
	}

	protected void setDigest(String digest) {
		this.digest = digest; 
	}
	
	public Track() {
		duration = -1;
		fileSize = -1;
		trackNumber = -1;
		type = TrackType.UNKNOWN;
		format = MediaFileFormat.UNKNOWN;
	}
	
	public Track(Map<String,String> properties) {
		this();
		setProperties(properties);
	}

	protected void setProperties(Map<String,String> properties) {
		// so we can see if we have leftovers, adds some inefficiency
		// though obviously...
		Map<String,String> copy = new HashMap<String,String>(properties);
		
		String s = copy.remove("type");
		if (s != null) {
			try {
				setType(TrackType.valueOf(s));
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid track type {}", s);
			}
		}
		s = copy.remove("format");
		if (s != null) {
			try {
				setFormat(MediaFileFormat.valueOf(s));
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid format type {}", s);
			}
		}
		s = copy.remove("name");
		if (s != null && s.length() > 0)
			setName(s);
		s = copy.remove("artist");
		if (s != null && s.length() > 0)
			setArtist(s);
		s = copy.remove("album");
		if (s != null && s.length() > 0)
			setAlbum(s);
		s = copy.remove("url");
		if (s != null && s.length() > 0) {
			try {
				// validate now to avoid problems later
				new URL(s);
				setUrl(s);
			} catch (MalformedURLException e) {
				logger.warn("Invalid track url {}", s);
			}
		}
		s = copy.remove("duration");
		if (s != null) {
			try {
				int v = Integer.parseInt(s);
				if (v < -1)
					logger.warn("Negative duration {}", v);
				else
					setDuration(v);
			} catch (NumberFormatException e) {
				logger.warn("Invalid track duration: {}", s);
			}
		}
		s = copy.remove("fileSize");
		if (s != null) {
			try {
				long v = Long.parseLong(s);
				if (v < -1)
					logger.warn("Negative fileSize {}", v);
				else
					setFileSize(v);
			} catch (NumberFormatException e) {
				logger.warn("Invalid track fileSize: {}", s);
			}
		}
		s = copy.remove("trackNumber");
		if (s != null) {
			try {
				int v = Integer.parseInt(s);
				if (v < -1 || v == 0)
					logger.warn("Invalid track number {}", v);
				else
					setTrackNumber(v);
			} catch (NumberFormatException e) {
				logger.warn("Invalid track number: {}", s);
			}
		}
		s = copy.remove("discIdentifier");
		if (s != null && s.length() > 0)
			setDiscIdentifier(s);
		
		if (copy.size() > 0) {
			logger.warn("Leftover properties setting up Track: {}", copy.keySet());
		}
		
		// locks the track so it can't be changed further
		computeDigest();
	}
	
	@Column(nullable=true)
	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getAlbum() {
		return album;
	}

	protected void setAlbum(String album) {
		this.album = album;
	}

	@Column(nullable=true)
	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getArtist() {
		return artist;
	}

	protected void setArtist(String artist) {
		this.artist = artist;
	}

	@Column(nullable=true)
	public String getDiscIdentifier() {
		return discIdentifier;
	}

	protected void setDiscIdentifier(String discIdentifier) {
		this.discIdentifier = discIdentifier;
	}

	@Column(nullable=false)
	public int getDuration() {
		return duration;
	}

	protected void setDuration(int duration) {
		this.duration = duration;
	}

	@Column(nullable=false)
	public long getFileSize() {
		return fileSize;
	}

	protected void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	@Column(nullable=false)
	public MediaFileFormat getFormat() {
		return format;
	}

	protected void setFormat(MediaFileFormat format) {
		this.format = format;
	}

	@Column(nullable=true)
	@Field(index=Index.TOKENIZED, store=Store.YES)
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Column(nullable=false)
	public int getTrackNumber() {
		return trackNumber;
	}

	protected void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}

	@Column(nullable=false)
	public TrackType getType() {
		return type;
	}

	protected void setType(TrackType type) {
		this.type = type;
	}

	@Column(nullable=true)
	public String getUrl() {
		return url;
	}

	protected void setUrl(String url) {
		this.url = url;
	}	
	
	@Override
	public String toString() {
		return "{Track id=" + getId() + " name=" + name + " album=" + album + " artist=" + artist + "}";
	}
	
	/* Should be final, except this makes Hibernate CGLIB enhancement barf */
	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Track))
			return false;
		Track other = (Track) arg0;
		if (other.digest != null && digest != null)
			return digest.equals(other.digest);
		else
			return super.equals(arg0);
	}

	/* Should be final, except this makes Hibernate CGLIB enhancement barf */	
	@Override
	public int hashCode() {
		if (digest != null)
			return digest.hashCode();
		else
			return super.hashCode();
	}
}
