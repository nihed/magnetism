package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

public final class AmazonReviews implements AmazonReviewsView {
	
	private int total;
	private int totalReviewPages;
	private List<AmazonReviewView> reviews;
	
	public AmazonReviews() {
		total = -1;
		totalReviewPages = -1;
		reviews = new ArrayList<AmazonReviewView>();
	}
	
	public List<? extends AmazonReviewView> getReviews() {
		return reviews;
	}
	
	void addReview(AmazonReview review) {
		reviews.add(review);
		// clear the stored total
		total = -1;
	}

	// when we get reviews from the web service we just count them to get the total, 
	// but when restoring from cache we potentially have the total stored
	// total might be different from reviews.size() if we didn't get all the reviews
	// written by the user, but know the total number of reviews
	public int getTotal() {
		if (total >= 0)
			return total;
		else
			return reviews.size();
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public int getTotalReviewPages() {
		return totalReviewPages;
	}
	
	public void setTotalReviewPages(int totalReviewPages) {
        this.totalReviewPages = totalReviewPages;
	}
	
	public void setReviews(List<? extends AmazonReviewView> reviews) {
		this.reviews.clear();
		this.reviews.addAll(reviews);
		// clear the stored total
		total = -1;
	} 
	
	@Override
	public String toString() {
		return "{AmazonReviews count=" + getTotal() + "}";
	}
}