package com.dumbhippo.persistence.caches;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.DBUnique;
import com.dumbhippo.services.AmazonList;
import com.dumbhippo.services.AmazonListView;
import com.dumbhippo.services.AmazonWebServices;

// AmazonList is both a CachedItem on its own, and a CachedListItem because it can
// be an item in a list of someone's Amazon lists
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"amazonUserId", "listId"})})
public class CachedAmazonList extends DBUnique implements CachedItem, CachedListItem {
	
	private static final Logger logger = GlobalSetup.getLogger(CachedAmazonList.class); 
	
	private static final long serialVersionUID = 1L;
	
	// an id of the list owner
	private String amazonUserId;
    private String listId;
    private String listName;
    private int totalItems;
    private int totalPages;
    private long dateCreated;   
	private long lastUpdated;
	
	// for hibernate
	protected CachedAmazonList() {	
	}
	
	public CachedAmazonList(String amazonUserId, String listId, String listName, int totalItems,
			                int totalPages, Date dateCreated) {
        this.amazonUserId = amazonUserId;
        this.listId = listId;
        this.listName = (listName == null ? "" : listName);
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.dateCreated = (dateCreated == null ? -1 : dateCreated.getTime());
    }
	
	static public CachedAmazonList newNoResultsMarker(String amazonUserId) {
		return CachedAmazonList.newNoResultsMarker(amazonUserId, "");
	}

	static public CachedAmazonList newNoResultsMarker(String amazonUserId, String listId) {
		return new CachedAmazonList(amazonUserId, listId, "", -1, -1, new Date(-1));
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalItems < 0;
	}
	
	public CachedAmazonList(String amazonUserId, AmazonListView list) {
		this(amazonUserId, list.getListId(), list.getListName(), list.getTotalItems(), list.getTotalPages(),
		     list.getDateCreated());
		if ((list.getAmazonUserId() != null) && !amazonUserId.equals(list.getAmazonUserId()))
			logger.warn("Created a CachedAmazonList where user {} was assigned a list that belongs to a different user {}", 
					    amazonUserId, list.getAmazonUserId());
	}
	
	public AmazonListView toAmazonList() {
	    AmazonList list = new AmazonList();
	    list.setAmazonUserId(amazonUserId);
	    list.setListId(listId);
	    list.setListName(listName);
	    list.setTotalItems(totalItems);
	    list.setTotalPages(totalPages);
	    list.setDateCreated(new Date(dateCreated));
		return list;
	}
	
	public void update(AmazonListView result) {
		if (!listId.equals(result.getListId()))
			throw new RuntimeException("Updating Amazon list " + listId + " with wrong result " + result.getListId());
		
        setListName(result.getListName());
        setTotalItems(result.getTotalItems());
        setTotalPages(result.getTotalPages());
        setDateCreated(result.getDateCreated());
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_USER_ID_LENGTH)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_LIST_ID_LENGTH)
	public String getListId() {
		return listId;
	}
	
	public void setListId(String listId) {
		this.listId = listId;
	}

	@Column(nullable=false)
	public String getListName() {
		return listName;
	}
	public void setListName(String listName) {
		this.listName = listName;
	}

	@Column(nullable=false)
	public int getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

	@Column(nullable=false)
	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	@Column(nullable=false)
	public Date getDateCreated() {
		return new Date(dateCreated);
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated.getTime();
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
			return "{CachedAmazonList:NoResultsMarker}";
		else
			return "{amazonUserId=" + amazonUserId + " listId=" + listId + " listName='" + listName + 
			       "' totalItems=" + totalItems + " totalPages=" + totalPages + 
			       " dateCreated=" + getDateCreated() + "}";
	}
}
