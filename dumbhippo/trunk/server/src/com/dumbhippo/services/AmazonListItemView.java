package com.dumbhippo.services;

import java.util.Date;

public interface AmazonListItemView {

	// not sure if this is useful for anything
	public String getListItemId();
	
	public String getItemId();
	
	public int getQuantityDesired();
	
	public int getQuantityReceived();
	
	public Date getDateAdded();
	
	// not sure if we'll want to include the comment when 
	// we display the notification
	public String getComment();
}
