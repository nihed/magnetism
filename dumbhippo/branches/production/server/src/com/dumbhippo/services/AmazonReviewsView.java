package com.dumbhippo.services;

import java.util.List;

public interface AmazonReviewsView {

	public List<? extends AmazonReviewView> getReviews();

	public int getTotal();

	public void addReview(AmazonReviewView review, boolean refreshTotal);
	
	public void addReviews(List<? extends AmazonReviewView> reviews, boolean refreshTotal);
	
	public void setReviews(List<? extends AmazonReviewView> reviews, boolean refreshTotal);
}