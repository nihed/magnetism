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
import com.dumbhippo.services.AmazonItem;
import com.dumbhippo.services.AmazonItemView;
import com.dumbhippo.services.AmazonWebServices;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"itemId"})})
public class CachedAmazonItem extends DBUnique implements CachedItem {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(CachedAmazonItem.class); 
	
	private static final long serialVersionUID = 1L;

	private String itemId;
	private String title;
	private String editorialReview;
	private String imageUrl;
	private int imageWidth;
	private int imageHeight;
	// prices are stored in format "$ddd.cc"
	private String newPrice;
	private String usedPrice;
	private String collectiblePrice;
	private String refurbishedPrice;
	private long lastUpdated;
	
	// for hibernate
	protected CachedAmazonItem() {	
	}
	
	public CachedAmazonItem(String itemId, String title, String editorialReview, String imageUrl,
			                int imageWidth, int imageHeight, String newPrice, String usedPrice,
			                String collectiblePrice, String refurbishedPrice) {
		setItemId(itemId);
		setTitle(title);
		setEditorialReview(editorialReview);
		setImageUrl(imageUrl);
		setImageWidth(imageWidth);
		setImageHeight(imageHeight);
		setNewPrice(newPrice);
		setUsedPrice(usedPrice);
		setCollectiblePrice(collectiblePrice);
		setRefurbishedPrice(refurbishedPrice);
    }
	
	static public CachedAmazonItem newNoResultsMarker(String itemId) {
		return new CachedAmazonItem(itemId, "", "", "", -1, -1, "", "", "", "");
	}
	
	@Transient
	public boolean isNoResultsMarker() {
		return title.trim().equals("");
	}
	
	public CachedAmazonItem(String itemId, AmazonItemView itemData) {
		this(itemId, itemData.getTitle(), itemData.getEditorialReview(), itemData.getImageUrl(),
			 itemData.getImageWidth(), itemData.getImageHeight(), itemData.getNewPrice(),
			 itemData.getUsedPrice(), itemData.getCollectiblePrice(), itemData.getRefurbishedPrice());
		if (!itemId.equals(itemData.getItemId()))
			logger.warn("Created a CachedAmazonItem where item with id {} was assigned attributes of an item with id {} that has title {}", 
					    new String[]{itemId, itemData.getItemId(), itemData.getTitle()});
	}
	
	public AmazonItemView toAmazonItem() {
	    AmazonItem item = new AmazonItem();
	    item.setItemId(itemId);
        item.setTitle(title);
        item.setEditorialReview(editorialReview);
        item.setImageUrl(imageUrl);
        item.setImageWidth(imageWidth);
        item.setImageHeight(imageHeight);
        item.setNewPrice(newPrice);
        item.setUsedPrice(usedPrice);
        item.setCollectiblePrice(collectiblePrice);
        item.setRefurbishedPrice(refurbishedPrice);
		return item;
	}
	
	public void update(AmazonItemView result) {
		if (!itemId.equals(result.getItemId()))
			throw new RuntimeException("Updating Amazon item " + itemId + " with wrong result " + result.getItemId());
		
        setTitle(result.getTitle());
        setEditorialReview(result.getEditorialReview());
        setImageUrl(result.getImageUrl());
        setImageWidth(result.getImageWidth());
        setImageHeight(result.getImageHeight());
        setNewPrice(result.getNewPrice());
        setUsedPrice(result.getUsedPrice());
        setCollectiblePrice(result.getCollectiblePrice());
        setRefurbishedPrice(result.getRefurbishedPrice());        
	}
	
	@Column(nullable=false, length=AmazonWebServices.MAX_AMAZON_ITEM_ID_LENGTH)
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	@Column(nullable=false)
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = (title == null ? "" : title);
	}
	
	@Column(nullable=false)
	public String getEditorialReview() {
		return editorialReview;
	}
	
	public void setEditorialReview(String editorialReview) {
		this.editorialReview = (editorialReview == null ? "" : editorialReview);
	}
	
	@Column(nullable=false)
	public String getImageUrl() {
		return imageUrl;
	}
	
	public void setImageUrl(String imageUrl) {
		this.imageUrl = (imageUrl == null ? "" : imageUrl);
	}
	
	@Column(nullable=false)
	public int getImageWidth() {
		return imageWidth;
	}
	
	public void setImageWidth(int imageWidth) {
	    this.imageWidth = imageWidth;	
	}
	
	@Column(nullable=false)
	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
	    this.imageHeight = imageHeight;	
	}
	
	@Column(nullable=false)
	public String getNewPrice() {
		return newPrice;
	}
	
	public void setNewPrice(String newPrice) {
		this.newPrice = (newPrice == null ? "" : newPrice);
	}
	
	@Column(nullable=false)
	public String getUsedPrice() {
		return usedPrice;
	}

	public void setUsedPrice(String usedPrice) {
		this.usedPrice = (usedPrice == null ? "" : usedPrice);
	}
	
	@Column(nullable=false)
	public String getCollectiblePrice() {
		return collectiblePrice;
	}
	
	public void setCollectiblePrice(String collectiblePrice) {
		this.collectiblePrice = (collectiblePrice == null ? "" : collectiblePrice);
	}
	
	@Column(nullable=false)
	public String getRefurbishedPrice() {
		return refurbishedPrice;
	}
	
	public void setRefurbishedPrice(String refurbishedPrice) {
		this.refurbishedPrice = (refurbishedPrice == null ? "" : refurbishedPrice);
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
			return "{CachedAmazonItem:NoResultsMarker}";
		else
			return "{itemId=" + itemId + " title=" + title + " newPrice=" + newPrice + "'}";
	}
}
