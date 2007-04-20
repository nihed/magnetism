package com.dumbhippo.services;

import java.util.List;

public interface AmazonReviewsView {

	public List<? extends AmazonReviewView> getReviews();

	public int getTotal();

	public void setReviews(List<? extends AmazonReviewView> reviews);
}