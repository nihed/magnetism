package com.dumbhippo.services;

import java.util.Date;

public interface AmazonReviewView {
	
	public String getAmazonUserId();
	
	public String getItemId(); 

	public int getRating();
	
	public int getHelpfulVotes();
	
	public int getTotalVotes();
	
	public String getTitle();
	
	public String getContent();
	
	public Date getReviewDate();
}
