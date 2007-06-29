package com.dumbhippo.services;

import java.util.Date;
import java.util.List;

public interface AmazonListView {

    public String getAmazonUserId();

    public String getListId();

    public String getListName();

    public int getTotalItems();

    public int getTotalPages();

    public Date getDateCreated();
    
	public List<? extends AmazonListItemView> getListItems();
	
	public void addListItem(AmazonListItemView listItem);
	
	public void addListItems(List<? extends AmazonListItemView> listItems);
	
	public void setListItems(List<? extends AmazonListItemView> listItems);

	public boolean hasListDetails();
	
	public boolean hasSomeItems();
	
    public String getListUrl();
}
