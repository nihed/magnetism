package com.dumbhippo.persistence;

import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;

@Entity
public class FacebookPhotoDataStatus extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookPhotoDataStatus.class);
	
	private FacebookAccount facebookAccount;
	private FacebookEvent facebookEvent;
	// a hex-encoded SHA-1 hash of the photoId is stored
	private String photoId;
	private Integer photoIdSalt;
	private CachedFacebookPhotoData photoData;
	
	protected FacebookPhotoDataStatus() {}
	
	public FacebookPhotoDataStatus(FacebookAccount facebookAccount, CachedFacebookPhotoData photoData) {
        this.facebookAccount = facebookAccount;
        this.photoData = photoData;
        setPhotoIdPlainText(photoData.getPhotoId());
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

	/**
	 * Get the salt for the photoId
	 * @return salt bytes as String
	 */
	@Column(nullable = false)
	public Integer getPhotoIdSalt() {
		// logger.debug("photoIdSalt is {}", photoIdSalt);
		return photoIdSalt;
	}

	/**
	 * Set the salt for the photoId
	 * @param photoIdSalt String containing the photoId salt
	 */
	public void setPhotoIdSalt(Integer photoIdSalt) {
		// logger.debug("setting photoIdSalt to {}", photoIdSalt);
		this.photoIdSalt = photoIdSalt;
	}

	/**
	 * Get the hash of the photoId.
	 * @return A String containing the hash of the photoId
	 */
	@Column(nullable = false)
	protected String getPhotoId() {
		return photoId;
	}

	/**
	 * Set the hash of the photoId.
	 * @param photoId A String with the hash of the photoId
	 */
	public void setPhotoId(String photoId) {
		this.photoId = photoId.trim(); // paranoia
	}

	/**
	 * Set hash photoId given a plaintext photoId, including
	 * generation of salt bytes.
	 * @param photoId
	 */
	public void setPhotoIdPlainText(String photoId) {
		if (photoId == null)
			throw new RuntimeException("photoId must not be null");
		
		Random random = new Random();		
		Integer photoIdSalt = random.nextInt();
		String photoIdHash = StringUtils.secureHash(photoId.trim(), photoIdSalt);
		setPhotoId(photoIdHash);
		setPhotoIdSalt(photoIdSalt);
	}
	
	/**
	 * Match the match photoId with the photoId for this photo data
	 * @param match A String with the plain text photoId
	 * @return A boolean true if the photoIds match
	 */
	public boolean matchPhotoId(String match) {
		String photoIdHash = getPhotoId();
		Integer photoIdSalt = getPhotoIdSalt();
		// logger.debug("match is {} photoIdSalt is {}", match, photoIdSalt);
		// logger.debug("photoIdHash is {}", photoIdHash);
		if (match == null || photoIdHash == null || photoIdSalt == null)
			return false;
		String matchHash = StringUtils.secureHash(match.trim(), photoIdSalt);
		// logger.debug("matchHash is {}", matchHash);
		return matchHash.equals(photoIdHash);
	}
	
	@OneToOne
	@JoinColumn(nullable=true)
	public CachedFacebookPhotoData getPhotoData() {
		return photoData;
	}

	// if we set the new photo data, we want it to match the photoId,
	// so we should call setNewPhotoData from the rest of our code 
	protected void setPhotoData(CachedFacebookPhotoData photoData) {
		this.photoData = photoData;
	}
	
	public void setNewPhotoData(CachedFacebookPhotoData photoData) {
		if ((photoData != null) && (!matchPhotoId(photoData.getPhotoId())))
			throw new RuntimeException("Updating a FacebookPhotoDataStatus " + this + 
					                   " with the the photoData that doesn't match the photo id: " + photoData);
		setPhotoData(photoData);
	}
	
	@Override
	public String toString() {
		return ("FacebookAccount: " + facebookAccount + " FacebookEvent " + facebookEvent 
				+ " CachedFacebookPhotoData: " + photoData);
	}
	
}
