package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmazonList implements AmazonListView {

    // an id of the list owner
    private String amazonUserId;
    private String listId;
    private String listName;
    private int totalItems;
    private int totalPages;
    private Date dateCreated;
    private List<AmazonListItemView> listItems;

    public AmazonList() {
        this.totalItems = -1;
        this.totalPages = -1;
        this.dateCreated = new Date(-1);
        this.listItems = new ArrayList<AmazonListItemView>();
    }

    public AmazonList(String amazonUserId, String listId, String listName,
                              int totalItems, int totalPages, Date dateCreated,
                              List<? extends AmazonListItemView> listItems) {
    	this();
        this.amazonUserId = amazonUserId;
        this.listId = listId;
        this.listName = listName;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.dateCreated = dateCreated;
		this.listItems.addAll(listItems);
    }

    public String getAmazonUserId() {
        return amazonUserId;
    }

    public void setAmazonUserId(String amazonUserId) {
        this.amazonUserId = amazonUserId;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
}

    public void setListName(String listName) {
        this.listName = listName;
    }

	// totalItems might be different from listItems.size() if we didn't get all the items
	// wish-listed by the user, but know the total number of items in the wish list
    public int getTotalItems() {
    	if (totalItems >= 0)
            return totalItems;
    	else 
    		return listItems.size();
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

	public List<? extends AmazonListItemView> getListItems() {
		return listItems;
	}
	
	public void addListItem(AmazonListItemView listItem) {
		listItems.add(listItem);
	}
	
	public void addListItems(List<? extends AmazonListItemView> listItems) {
		this.listItems.addAll(listItems);
	}
	
	public void setListItems(List<? extends AmazonListItemView> listItems) {
		this.listItems.clear();
		this.listItems.addAll(listItems);
	}
	
	public boolean hasListDetails() {
	    return (totalItems != -1);	
	}
	
	public boolean hasSomeItems() {
		return (listItems.size() > 0);
	}
	
    public String getListUrl() {
        // TODO: find out if it is useful to attach the associate id to the
        // wishlist link
        return "http://www.amazon.com/gp/registry/" + getListId();
    }
}