package com.dumbhippo.services;


public interface AmazonItemView {
	
	// we shouldn't keep around a longer version of editorial review
	// because we might end up displaying a longer version before we put 
	// it into the database and it gets truncated 
	public static int MAX_EDITORIAL_REVIEW_LENGTH = 255;
	
	String getItemId();
	
	String getTitle();
	
	String getEditorialReview();
	
	String getImageUrl();
	
	int getImageWidth();
	
	int getImageHeight();
	
	String getNewPrice();
	
	String getUsedPrice();
	
	String getCollectiblePrice();
	
	String getRefurbishedPrice();
	
}
