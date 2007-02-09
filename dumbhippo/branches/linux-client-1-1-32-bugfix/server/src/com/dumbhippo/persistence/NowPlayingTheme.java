package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dumbhippo.server.Configuration;

/**
 * A theme for the "now playing" flash embed.
 * 
 * @author Havoc Pennington
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class NowPlayingTheme extends EmbeddedGuidPersistable {

	static final private String BLUE = "#0000FF";
	
	private NowPlayingTheme basedOn;
	private User creator;
	private long creationDate;
	private String name;
	private String activeImage;
	private String inactiveImage;
	private int albumArtX;
	private int albumArtY;
	private int titleTextX;
	private int titleTextY;
	private String titleTextColor;
	private int titleTextFontSize;
	private int artistTextX;
	private int artistTextY;
	private String artistTextColor;
	private int artistTextFontSize;
	private int albumTextX;
	private int albumTextY;
	private String albumTextColor;
	private int albumTextFontSize;
	private int statusTextX;
	private int statusTextY;
	private String statusTextColor;
	private int statusTextFontSize;
	private boolean draft;
	
	static public String toFilename(String mode, String shaSum) {
		return String.format("%dx%d/%s",
				Configuration.NOW_PLAYING_THEME_WIDTH, Configuration.NOW_PLAYING_THEME_HEIGHT,
				shaSum);
	}
	
	static public String toRelativeUrl(String mode, String shaSum) {
		return "/files" + Configuration.NOW_PLAYING_THEMES_RELATIVE_PATH + "/" + toFilename(mode, shaSum);
	}
	
	/**
	 * Constructor for use by hibernate only
	 *
	 */
	public NowPlayingTheme() {
		this(null);
	}

	private NowPlayingTheme(NowPlayingTheme basedOn) {
		this.name = "Untitled";
		this.draft = true;
		
		if (basedOn != null) {
			this.basedOn = basedOn;
			
			activeImage = basedOn.activeImage;
			inactiveImage = basedOn.inactiveImage;
			albumArtX = basedOn.albumArtX;
			albumArtY = basedOn.albumArtY;
			titleTextX = basedOn.titleTextX;
			titleTextY = basedOn.titleTextY;
			titleTextColor = basedOn.titleTextColor;
			titleTextFontSize = basedOn.titleTextFontSize;
			artistTextX = basedOn.artistTextX;
			artistTextY = basedOn.artistTextY;
			artistTextColor = basedOn.artistTextColor;
			artistTextFontSize = basedOn.albumTextFontSize;
			albumTextX = basedOn.albumTextX;
			albumTextY = basedOn.albumTextY;
			albumTextColor = basedOn.albumTextColor;
			albumTextFontSize = basedOn.albumTextFontSize;
			statusTextX = basedOn.statusTextX;
			statusTextY = basedOn.statusTextY;
			statusTextColor = basedOn.statusTextColor;
			statusTextFontSize = basedOn.statusTextFontSize;
			
		} else {
			this.titleTextColor = BLUE;
			this.artistTextColor = BLUE;
			this.albumTextColor = BLUE;
			this.statusTextColor = BLUE;
			
			// some arbitrary defaults so we get a sane starting point
			this.albumArtX = 102;
			this.albumArtY = 36;
			this.statusTextX = 0;
			this.statusTextY = 102;
			this.statusTextFontSize = 12;
			this.artistTextX = 178;
			this.artistTextY = 63;
			this.artistTextFontSize = 14;
			this.titleTextX = 176;
			this.titleTextY = 41;
			this.titleTextFontSize = 20;
			this.albumTextX = 450; 
			this.albumTextY = 300;
			this.albumTextFontSize = 14;
		}
	}
	
	/** 
	 * Use this constructor from your code
	 * 
	 * @param basedOn null or a theme to base this one on
	 * @param creator user creating the theme (must not be null)
	 */
	public NowPlayingTheme(NowPlayingTheme basedOn, User creator) {
		this(basedOn);
		
		this.creationDate = System.currentTimeMillis();
		this.creator = creator;
	}

	/**
	 * This is the sha1sum of the image, which is used to determine 
	 * the image's filename. We do this instead of storing images by 
	 * theme guid so that themes can share images.
	 * 
	 * @return the sha1sum of the image in hex format
	 */
	@Column(nullable=true)
	public String getActiveImage() {
		return activeImage;
	}

	public void setActiveImage(String activeImage) {
		this.activeImage = activeImage;
	}

	@Transient
	public String getActiveImageRelativeUrl() {
		if (activeImage == null)
			return null;
		else
			return toRelativeUrl("active", activeImage);
	}
	
	@Column(nullable=false)
	public int getAlbumArtX() {
		return albumArtX;
	}

	public void setAlbumArtX(int albumArtX) {
		this.albumArtX = albumArtX;
	}

	@Column(nullable=false)
	public int getAlbumArtY() {
		return albumArtY;
	}

	public void setAlbumArtY(int albumArtY) {
		this.albumArtY = albumArtY;
	}

	@Column(nullable=false)
	public String getAlbumTextColor() {
		return albumTextColor;
	}

	public void setAlbumTextColor(String albumTextColor) {
		this.albumTextColor = albumTextColor;
	}

	@Column(nullable=false)
	public int getAlbumTextX() {
		return albumTextX;
	}

	public void setAlbumTextX(int albumTextX) {
		this.albumTextX = albumTextX;
	}

	@Column(nullable=false)
	public int getAlbumTextY() {
		return albumTextY;
	}

	public void setAlbumTextY(int albumTextY) {
		this.albumTextY = albumTextY;
	}

	@Column(nullable=false)
	public String getArtistTextColor() {
		return artistTextColor;
	}

	public void setArtistTextColor(String artistTextColor) {
		this.artistTextColor = artistTextColor;
	}

	@Column(nullable=false)
	public int getArtistTextX() {
		return artistTextX;
	}

	public void setArtistTextX(int artistTextX) {
		this.artistTextX = artistTextX;
	}

	@Column(nullable=false)
	public int getArtistTextY() {
		return artistTextY;
	}

	public void setArtistTextY(int artistTextY) {
		this.artistTextY = artistTextY;
	}

	@JoinColumn(nullable=true)
	@ManyToOne
	public NowPlayingTheme getBasedOn() {
		return basedOn;
	}

	public void setBasedOn(NowPlayingTheme basedOn) {
		this.basedOn = basedOn;
	}

	@JoinColumn(nullable=false)
	@ManyToOne
	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	@Column(nullable=true)
	public String getInactiveImage() {
		return inactiveImage;
	}

	public void setInactiveImage(String inactiveImage) {
		this.inactiveImage = inactiveImage;
	}

	@Transient
	public String getInactiveImageRelativeUrl() {
		if (inactiveImage == null)
			return null;
		else
			return toRelativeUrl("inactive", inactiveImage);
	}
	
	@Column(nullable=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(nullable=false)
	public String getTitleTextColor() {
		return titleTextColor;
	}

	public void setTitleTextColor(String titleTextColor) {
		this.titleTextColor = titleTextColor;
	}

	@Column(nullable=false)
	public int getTitleTextX() {
		return titleTextX;
	}

	public void setTitleTextX(int titleTextX) {
		this.titleTextX = titleTextX;
	}

	@Column(nullable=false)
	public int getTitleTextY() {
		return titleTextY;
	}

	public void setTitleTextY(int titleTextY) {
		this.titleTextY = titleTextY;
	}
	
	@Column(nullable=false)
	public Date getCreationDate() {
		return new Date(creationDate);
	}
	
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate.getTime();
	}

	@Column(nullable=false)
	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	
	@Override
	public String toString() {
		return "{NowPlayingTheme " + getId() + "}";
	}

	@Column(nullable=false)
	public String getStatusTextColor() {
		return statusTextColor;
	}

	public void setStatusTextColor(String statusTextColor) {
		this.statusTextColor = statusTextColor;
	}

	@Column(nullable=false)
	public int getStatusTextX() {
		return statusTextX;
	}

	public void setStatusTextX(int statusTextX) {
		this.statusTextX = statusTextX;
	}

	@Column(nullable=false)
	public int getStatusTextY() {
		return statusTextY;
	}

	public void setStatusTextY(int statusTextY) {
		this.statusTextY = statusTextY;
	}

	@Column(nullable=false)
	public int getAlbumTextFontSize() {
		return albumTextFontSize;
	}

	public void setAlbumTextFontSize(int albumTextFontSize) {
		this.albumTextFontSize = albumTextFontSize;
	}

	@Column(nullable=false)
	public int getArtistTextFontSize() {
		return artistTextFontSize;
	}

	public void setArtistTextFontSize(int artistTextFontSize) {
		this.artistTextFontSize = artistTextFontSize;
	}

	@Column(nullable=false)
	public int getStatusTextFontSize() {
		return statusTextFontSize;
	}

	public void setStatusTextFontSize(int statusTextFontSize) {
		this.statusTextFontSize = statusTextFontSize;
	}

	@Column(nullable=false)
	public int getTitleTextFontSize() {
		return titleTextFontSize;
	}

	public void setTitleTextFontSize(int titleTextFontSize) {
		this.titleTextFontSize = titleTextFontSize;
	}
}
