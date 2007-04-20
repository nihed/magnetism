package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.AmazonReviews;
import com.dumbhippo.services.AmazonReviewsView;
import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"amazonUserId"})})
public class CachedAmazonReviews extends DBUnique implements CachedItem {

	private static final long serialVersionUID = 1L;
	
	private String amazonUserId;
	private int totalCount;
	private long lastUpdated;
	
	protected CachedAmazonReviews() {
		
	}
	
	public CachedAmazonReviews(String amazonUserId, int totalCount) {
		this.amazonUserId = amazonUserId;
		this.totalCount = totalCount;
	}
	
	public CachedAmazonReviews(String amazonUserId, AmazonReviewsView view) {
		this.amazonUserId = amazonUserId;
		update(view);
	}
	
	static public CachedAmazonReviews newNoResultsMarker(String amazonUserId) {
		return new CachedAmazonReviews(amazonUserId, -1);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalCount < 0;
	}	

	public void update(AmazonReviewsView reviews) {
		if (reviews == null)
			totalCount = -1; // no results marker
		else
			setTotalCount(reviews.getTotal());
	}
	
	public AmazonReviewsView toAmazonReviews() {
		AmazonReviews amazonReviews = new AmazonReviews();
		amazonReviews.setTotal(totalCount);
		return amazonReviews;
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_USER_ID_LENGTH)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}
	
	@Column(nullable=false)
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}	
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedAmazonReview:NoResultsMarker}";
		else
			return "{amazonUserId=" + amazonUserId + "}";
	}
}