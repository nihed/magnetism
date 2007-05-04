package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.AmazonLists;
import com.dumbhippo.services.AmazonListsView;
import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"amazonUserId"})})
public class CachedAmazonLists extends DBUnique implements CachedItem {

	private static final long serialVersionUID = 1L;
	
	private String amazonUserId;
	private int totalCount;
	private long lastUpdated;
	
	protected CachedAmazonLists() {
		
	}
	
	public CachedAmazonLists(String amazonUserId, int totalCount) {
		this.amazonUserId = amazonUserId;
		this.totalCount = totalCount;
	}
	
	public CachedAmazonLists(String amazonUserId, AmazonListsView view) {
		this.amazonUserId = amazonUserId;
		update(view);
	}
	
	static public CachedAmazonLists newNoResultsMarker(String amazonUserId) {
		return new CachedAmazonLists(amazonUserId, -1);
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return totalCount < 0;
	}	

	public void update(AmazonListsView lists) {
		if (lists == null)
			totalCount = -1; // no results marker
		else
			setTotalCount(lists.getTotal());
	}
	
	public AmazonListsView toAmazonLists() {
		AmazonLists amazonLists = new AmazonLists();
		amazonLists.setTotal(totalCount);
		return amazonLists;
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_USER_ID_LENGTH)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}
	
	@Column(nullable=false)
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
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
			return "{CachedAmazonLists:NoResultsMarker}";
		else
			return "{CachedAmazonLists: amazonUserId=" + amazonUserId +
			       " totalCount=" + totalCount + "}";
	}
}