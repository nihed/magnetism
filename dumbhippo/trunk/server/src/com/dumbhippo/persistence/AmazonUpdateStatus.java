package com.dumbhippo.persistence;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CollectionOfElements;

import com.dumbhippo.services.AmazonWebServices;

/** 
 * Records persistent state of Amazon polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class AmazonUpdateStatus extends DBUnique {
	
	private String amazonUserId;
	private String reviewsHash;
	private Map<String, String> listHashes;
	
	AmazonUpdateStatus() {
	}
	
	public AmazonUpdateStatus(String amazonUserId) {
		this.amazonUserId = amazonUserId;
		reviewsHash = "";
		listHashes = new HashMap<String, String>();
	}
	
	@Column(nullable=false, unique=true)
	public String getAmazonUserId() {
		return amazonUserId;
	}
	
	public void setAmazonUserId(String amazonUserId) {
		this.amazonUserId = amazonUserId;
	}
	
	@Column(nullable=false)
	public String getReviewsHash() {
		return reviewsHash;
	}
	
	public void setReviewsHash(String reviewsHash) {
		this.reviewsHash = reviewsHash;
	}

    @CollectionOfElements
    @JoinTable(name="AmazonUpdateStatus_ListHash",
               joinColumns = @JoinColumn(name="amazonUpdateStatus_id"))
    @MapKey(columns={@Column(name="listId", length=AmazonWebServices.MAX_AMAZON_LIST_ID_LENGTH)})
    @Column(name="listHash", nullable=false)
	public Map<String, String> getListHashes() {
		return listHashes;
	}
	
	public void setListHashes(Map<String, String> listHashes) {
		if (listHashes == null)
			throw new IllegalArgumentException("null listHashes");
		this.listHashes = listHashes;
	}
	
	public void putListHash(String listId, String listHash) {
		listHashes.put(listId, listHash);
	}
	
	public void removeListHash(String listId) {
		listHashes.remove(listId);
	}
}