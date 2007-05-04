package com.dumbhippo.services;

import java.util.Date;

public class AmazonReview implements AmazonReviewView {
    
    // an id of the reviewer
	private String amazonUserId;
	private String itemId;
	private int rating;
	private int helpfulVotes;
	private int totalVotes;
	private String title;
	private String content;
	private Date reviewDate;
	
	public AmazonReview() {
		rating = -1;
		helpfulVotes = -1;
		totalVotes = -1;
		reviewDate = new Date(-1);
	}

	public AmazonReview(String amazonUserId, String itemId, int rating, int helpfulVotes, int totalVotes,
	                    String title, String content, Date reviewDate) {
		this();
		this.amazonUserId = amazonUserId;
		this.itemId = itemId;
		this.rating = rating;
		this.helpfulVotes = helpfulVotes;
		this.totalVotes = totalVotes;
		this.title = title;
		this.content = content;
		this.reviewDate = reviewDate;
	}
	
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}
	
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}
	
	public int getHelpfulVotes() {
		return helpfulVotes;
	}

	public void setHelpfulVotes(int helpfulVotes) {
		this.helpfulVotes = helpfulVotes;
	}
	
	public int getTotalVotes() {
		return totalVotes;
	}
	
	public void setTotalVotes(int totalVotes) {
		this.totalVotes = totalVotes;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getReviewDate() {
		return reviewDate;
	}

	public void setReviewDate(Date reviewDate) {
		this.reviewDate = reviewDate;
	}
}
