package com.dumbhippo.services;

import java.util.Date;

public interface AmazonListItemView {

	// this is useful for creating link to the
	// product to be purchased for someone's wish list
	public String getListItemId();
	
	public String getItemId();
	
	public int getQuantityDesired();
	
	public int getQuantityReceived();
	
	public Date getDateAdded();

	public String getComment();
}
