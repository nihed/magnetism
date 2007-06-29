package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.services.AmazonListItem;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"listId", "itemId"})})
public class CachedAmazonListItem extends DBUnique implements CachedListItem {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(CachedAmazonListItem.class); 
	
	private static final long serialVersionUID = 1L;

	private String listId;
	private String itemId;
	private String listItemId;
	private int quantityDesired;
	private int quantityReceived;
	private long dateAdded;
	private String comment;  
	private long lastUpdated;
	
	// for hibernate
	protected CachedAmazonListItem() {	
	}
	
	public CachedAmazonListItem(String listId, String itemId, String listItemId, int quantityDesired,
			                    int quantityReceived, Date dateAdded, String comment) {
        this.listId = listId;
        this.itemId = itemId;
        this.listItemId = listItemId;
        this.quantityDesired = quantityDesired;
        this.quantityReceived = quantityReceived;
        this.dateAdded = (dateAdded == null ? -1 : dateAdded.getTime());
        this.comment = (comment == null ? "" : comment);
    }
	
	static public CachedAmazonListItem newNoResultsMarker(String listId) {
		return new CachedAmazonListItem(listId, "", "", -1, -1, new Date(-1), "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return itemId.trim().equals("");
	}
	
	public CachedAmazonListItem(String listId, AmazonListItemView listItem) {
		this(listId, listItem.getItemId(), listItem.getListItemId(), listItem.getQuantityDesired(),
			 listItem.getQuantityReceived(), listItem.getDateAdded(), listItem.getComment());
	}
	
	public AmazonListItemView toAmazonListItem() {
	    AmazonListItem listItem = new AmazonListItem();
	    listItem.setItemId(itemId);
	    listItem.setListItemId(listItemId);
	    listItem.setQuantityDesired(quantityDesired);
	    listItem.setQuantityReceived(quantityReceived);
	    listItem.setDateAdded(new Date(dateAdded));
	    listItem.setComment(comment);
		return listItem;
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_LIST_ID_LENGTH)
	public String getListId() {
		return listId;
	}
	
	public void setlistId(String listId) {
		this.listId = listId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_ITEM_ID_LENGTH)
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_LIST_ITEM_ID_LENGTH)
	public String getListItemId() {
		return listItemId;
	}
	
	public void setListItemId(String listItemId) {
		this.listItemId = listItemId;
	}
	
	@Column(nullable=false)
	public int getQuantityDesired() {
		return quantityDesired;
	}

	public void setQuantityDesired(int quantityDesired) {
		this.quantityDesired = quantityDesired;
	}
	
	@Column(nullable=false)
	public int getQuantityReceived() {
		return quantityReceived;
	}

	public void setQuantityReceived(int quantityReceived) {
		this.quantityReceived = quantityReceived;
	}

	@Column(nullable=false)
	public Date getDateAdded() {
		return new Date(dateAdded);
	}

	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded.getTime();
	}	
	
	@Column(nullable=false)
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}	
	
	@Override
	public String toString() {
		if (isNoResultsMarker())
			return "{CachedAmazonListItem:NoResultsMarker}";
		else
			return "{listId=" + listId + "itemId=" + itemId + " listItemId=" + listItemId 
			       + " quantityDesired=" + quantityDesired + " quantityReceived=" + quantityReceived
			       + " dateAdded=" + getDateAdded() + " comment='" + comment + "'}";
	}
}
