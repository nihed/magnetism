package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.List;

public final class AmazonReviews implements AmazonReviewsView {
	
	private int total;
	private int totalReviewPages;
	private List<AmazonReviewView> reviews;
	
	public AmazonReviews() {
		this(-1, -1);
	}
	
	public AmazonReviews(int total, int totalReviewPages) {		
		this.total = total;
		this.totalReviewPages = totalReviewPages;
		reviews = new ArrayList<AmazonReviewView>();
	}
	
	public List<? extends AmazonReviewView> getReviews() {
		return reviews;
	}
	
	public void addReview(AmazonReviewView review, boolean refreshTotal) {
		reviews.add(review);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	}
	
	public void addReviews(List<? extends AmazonReviewView> reviews, boolean refreshTotal) {
		this.reviews.addAll(reviews);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	}

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
	
	public void setReviews(List<? extends AmazonReviewView> reviews, boolean refreshTotal) {
		this.reviews.clear();
		this.reviews.addAll(reviews);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	} 
	
	@Override
	public String toString() {
		return "{AmazonReviews count=" + getTotal() + "}";
	}
}