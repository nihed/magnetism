package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** 
 * Records persistent state of Amazon polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class AmazonUpdateStatus extends DBUnique {

	private String amazonUserId;
	private String reviewsHash;
	private String wishListsHash;
	
	AmazonUpdateStatus() {
	}
	
	public AmazonUpdateStatus(String amazonUserId) {
		this.amazonUserId = amazonUserId;
		reviewsHash = "";
		wishListsHash = "";
	}
	
	@Column(nullable=false,unique=true)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}
	
	@Column(nullable=false)
	public String getReviewsHash() {
		return reviewsHash;
	}
	
	public void setReviewsHash(String reviewsHash) {
		this.reviewsHash = reviewsHash;
	}
	
	@Column(nullable=false)
	public String getWishListsHash() {
		return wishListsHash;
	}
	
	public void setWishListsHash(String wishListsHash) {
		this.wishListsHash = wishListsHash;
	}	
}