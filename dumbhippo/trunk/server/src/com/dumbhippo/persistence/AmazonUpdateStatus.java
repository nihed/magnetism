package com.dumbhippo.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CollectionOfElements;

/** 
 * Records persistent state of Amazon polling.  See FlickrUpdateStatus.
 */
@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class AmazonUpdateStatus extends DBUnique {

	private String amazonUserId;
	private String reviewsHash;
	private Set<String> listHashes;
	
	AmazonUpdateStatus() {
	}
	
	public AmazonUpdateStatus(String amazonUserId) {
		this.amazonUserId = amazonUserId;
		reviewsHash = "";
		listHashes = new HashSet<String>();
	}
	
	@Column(nullable=false,unique=true)
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
    @Column(name="listHash")
	public Set<String> getListHashes() {
		return listHashes;
	}
	
	public void setListHashes(Set<String> listHashes) {
		if (listHashes == null)
			throw new IllegalArgumentException("null listHashes");
		this.listHashes = listHashes;
	}
	
	public void addListHash(String listHash) {
		listHashes.add(listHash);
	}
	
	public void removeListHash(String listHash) {
		listHashes.remove(listHash);
	}
	
	@Transient
	public Map<String, String> getListHashesMap() {
		Map<String, String> listHashesMap = new HashMap<String, String>();
		for (String listHash : getListHashes()) {
			// we expect all listHash items to have the list id and a "-" following it
			listHashesMap.put(listHash.substring(0, listHash.indexOf("-")), listHash);
		}
		return listHashesMap;
	}
}