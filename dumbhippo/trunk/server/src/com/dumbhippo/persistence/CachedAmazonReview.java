package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.services.AmazonReview;
import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"amazonUserId", "itemId"})})
public class CachedAmazonReview extends DBUnique implements CachedListItem {
	
	private static final Logger logger = GlobalSetup.getLogger(CachedAmazonReview.class); 
	
	private static final long serialVersionUID = 1L;
	
	// an id of the reviewer
	private String amazonUserId;
	private String itemId;
	private int rating;
	private int helpfulVotes;
	private int totalVotes;
	private String title;
	private String content;
    private long reviewDate;    
	private long lastUpdated;
	
	// for hibernate
	protected CachedAmazonReview() {	
	}
	
	public CachedAmazonReview(String amazonUserId, String itemId, int rating, int helpfulVotes, int totalVotes,
                              String title, String content, Date reviewDate) {
        this.amazonUserId = amazonUserId;
        this.itemId = itemId;
        this.rating = rating;
        this.helpfulVotes = helpfulVotes;
        this.totalVotes = totalVotes;
        this.title = title;
        this.content = content;
        this.reviewDate = reviewDate.getTime();
    }
	
	static public CachedAmazonReview newNoResultsMarker(String amazonUserId) {
		return new CachedAmazonReview(amazonUserId, "", -1, -1, -1, "", "", new Date(-1));
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return itemId.trim().equals("");
	}
	
	public CachedAmazonReview(String amazonUserId, AmazonReviewView review) {
		this(amazonUserId, review.getItemId(), review.getRating(), review.getHelpfulVotes(), review.getTotalVotes(),
				 review.getTitle(), review.getContent(), review.getReviewDate());
		if (amazonUserId != review.getAmazonUserId())
			logger.warn("Created a CachedAmazonReview where user {} owns a review written by a different user {} for item {}", 
					    new String[]{amazonUserId, review.getAmazonUserId(), review.getItemId()});
	}
	
	public AmazonReviewView toAmazonReview() {
	    AmazonReview review = new AmazonReview();
	    review.setAmazonUserId(amazonUserId);
	    review.setItemId(itemId);
	    review.setRating(rating);
	    review.setHelpfulVotes(helpfulVotes);
	    review.setTotalVotes(totalVotes);
	    review.setTitle(title);
	    review.setContent(content);
	    review.setReviewDate(new Date(reviewDate));
		return review;
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_USER_ID_LENGTH)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_ITEM_ID_LENGTH)
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	
	@Column(nullable=false)
	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}
	
	public void setHelpfulVotes(int helpfulVotes) {
		this.helpfulVotes = helpfulVotes;
	}

	@Column(nullable=false)
	public int getHelpfulVotes() {
		return helpfulVotes;
	}

	public void setTotalVotes(int totalVotes) {
		this.totalVotes = totalVotes;
	}
	
	@Column(nullable=false)
	public int getTotalVotes() {
		return totalVotes;
	}

	@Column(nullable=false)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(nullable=false)
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	@Column(nullable=false)
	public Date getReviewDate() {
		return new Date(reviewDate);
	}

	public void setReviewDate(Date reviewDate) {
		this.reviewDate = reviewDate.getTime();
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
			return "{amazonUserId=" + amazonUserId + "itemId=" + itemId + " rating=" + rating + " title='" + title + "'}";
	}
}
