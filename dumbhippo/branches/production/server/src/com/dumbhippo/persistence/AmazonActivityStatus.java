package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"amazonUserId","itemId","listId","activityType"})})
public class AmazonActivityStatus extends EmbeddedGuidPersistable {
    private String amazonUserId;
    private String itemId;
    private String listId;
    private AmazonActivityType activityType;
    // TODO: see if this is actually useful
    // private long activityDate;
    
	protected AmazonActivityStatus() {
	}
	
	public AmazonActivityStatus(String amazonUserId, String itemId, String listId, AmazonActivityType activityType) {
		this.amazonUserId = amazonUserId;
		this.itemId = itemId;
		this.listId = listId;
		this.activityType = activityType;
		// this.activityDate = activityDate.getTime();
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_USER_ID_LENGTH)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_ITEM_ID_LENGTH)
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_LIST_ID_LENGTH)
	public String getListId() {
		return listId;
	}
	
	public void setListId(String listId) {
		this.listId = listId;
	}
	
	@Column(nullable=false)
	public AmazonActivityType getActivityType() {
		return activityType;
	}
	
	public void setActivityType(AmazonActivityType activityType) {
		this.activityType = activityType;
	}
	
	/*
	@Column(nullable=false)
	public Date getActivityDate() {
		return new Date(activityDate);
	}

	public void setActivityDate(Date activityDate) {
		this.activityDate = activityDate.getTime();
	}
	*/	
	
	@Override
	public String toString() {
		return "{guid=" + getId() + " amazonUserId=" + amazonUserId + " itemId=" + itemId + 
		       " listId=" + listId + " activityType=" + activityType + "}";
	}
}
