package com.dumbhippo.persistence;

import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

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
	// a hex-encoded SHA-1 hash of the photoId is stored, deprecated
	private String photoId;
	// deprecated
	private Integer photoIdSalt;
	
	// We store the facebook "photo ID" to try and uniquely identify a photo over time.
	// However this isn't perfect: facebook photo ID's are only unique among photos
	// uploaded by a particular user. So we could have the situation where our user
	// is tagged in two photos with the same photo ID uploaded by different facebook
	// users. If that happens, we'll throw a constaint violation when trying to save
	// the photos to CachedFacebookPhotoData.java. Luckily, this situation seems
	// to occur very infrequently.
	//
	// The way to fix this is to store not just the facebook photo ID
	// but the facebook photo ID and the ID of the facebook user that uploaded the
	// photo. (A transitional measure after adding the uploading user ID is to consider a 
	// FacebookPhotoDataStatus with no uploading user ID as matching any photo 
	// with the same photo ID.)
	private String facebookPhotoId;
	
	protected FacebookPhotoDataStatus() {}
	
	public FacebookPhotoDataStatus(FacebookAccount facebookAccount, String facebookPhotoId) {
        this.facebookAccount = facebookAccount;
        this.facebookPhotoId = facebookPhotoId;
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
	@Column(nullable = true)
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
	@Column(nullable = true)
	protected String getPhotoId() {
		return photoId;
	}

	/**
	 * Set the hash of the photoId.
	 * @param photoId A String with the hash of the photoId
	 */
	public void setPhotoId(String photoId) {
		if (photoId != null)
		    this.photoId = photoId.trim(); // paranoia
		else
			this.photoId = null;
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
	@Transient
	public boolean matchPhotoId(String match) {
		// to support the transition from using encoded ids to using 
		// actual photo ids, if facebookPhotoId is not null, we do the match 
		// based on that, if it is null, we do the match based on the encoded id
		// if that doesn't work
		if (facebookPhotoId != null)
			return (facebookPhotoId.equals(match));
		
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
	
	@Column(nullable = true)
	public String getFacebookPhotoId() {
		return facebookPhotoId;
	}

	public void setFacebookPhotoId(String facebookPhotoId) {
		this.facebookPhotoId = facebookPhotoId;
	}
	
	// we should use this function to make sure we transition correctly
	public void setRecoveredFacebookPhotoId(String facebookPhotoId) {
		if (!matchPhotoId(facebookPhotoId))
			throw new RuntimeException("Updating a FacebookPhotoDataStatus " + this + 
					                   " with the new facebookPhotoId " + facebookPhotoId + " that doesn't match the original photo id");
		setFacebookPhotoId(facebookPhotoId);
		setPhotoId(null);
		setPhotoIdSalt(null);
	}
	
	@Override
	public String toString() {
		return ("FacebookAccount: " + facebookAccount + " FacebookEvent " + facebookEvent 
				+ " facebookPhotoId: " + facebookPhotoId);
	}
	
}
