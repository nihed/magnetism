package com.dumbhippo.services;

import java.util.Date;

public class AmazonListItem implements AmazonListItemView {
	
	private String listItemId;
	private String itemId;
	private int quantityDesired;
	private int quantityReceived;
	private Date dateAdded;
	private String comment;

	public AmazonListItem() {
		this.quantityDesired = -1;
		this.quantityReceived = -1;
		this.dateAdded = new Date(-1);
	}
	
	public AmazonListItem(String listItemId, String itemId, int quantityDesired, 
			              int quantityReceived, Date dateAdded, String comment) {
		this();
	    this.listItemId = listItemId;
	    this.itemId = itemId;
	    this.quantityDesired = quantityDesired;
	    this.quantityReceived = quantityReceived;
	    this.dateAdded = dateAdded;
	    this.comment = comment;
	}
	
	public String getListItemId() {
        return listItemId;		
	}
	
	public void setListItemId(String listItemId) {
		this.listItemId = listItemId;
	}
	
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	
	public int getQuantityDesired() {
	    return quantityDesired;	
	}
	
	public void setQuantityDesired(int quantityDesired) {
		this.quantityDesired = quantityDesired;
	}
	
	public int getQuantityReceived() {
		return quantityReceived;
	}
	
	public void setQuantityReceived(int quantityReceived) {
		this.quantityReceived = quantityReceived;
	}
	
	public Date getDateAdded() {
	    return dateAdded;	
	}
	
	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}

	public String getComment() {
	    return comment;	
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
}
