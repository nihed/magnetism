package com.dumbhippo.services;

public class AmazonItem implements AmazonItemView {
	private String itemId;
	private String title;
	private String editorialReview;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	private String newPrice;
	private String usedPrice;
	private String collectiblePrice;
	private String refurbishedPrice;

	public AmazonItem() {
		smallImageWidth = -1;
		smallImageHeight = -1;
	}
	
	public AmazonItem(String itemId, String title, String editorialReview, String smallImageUrl,
			          int smallImageWidth, int smallImageHeight, String newPrice, String usedPrice,
			          String collectiblePrice, String refurbishedPrice) {
		this.itemId = itemId;
		this.title = title;
		this.editorialReview = editorialReview;
		this.smallImageUrl = smallImageUrl;
		this.smallImageWidth = smallImageWidth;
		this.smallImageHeight = smallImageHeight;
		this.newPrice = newPrice;
		this.usedPrice = usedPrice;
		this.collectiblePrice = collectiblePrice;
		this.refurbishedPrice = refurbishedPrice;		
	}
	
	public String getItemId() {
		return itemId;
	}
	
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getEditorialReview() {
		return editorialReview;
	}
	
	public void setEditorialReview(String editorialReview) {
		this.editorialReview = editorialReview;
	}

	public String getSmallImageUrl() {
		return smallImageUrl;
	}
	
	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}
	
	public int getSmallImageWidth() {
		return smallImageWidth;
	}
	
	public void setSmallImageWidth(int smallImageWidth) {
	    this.smallImageWidth = smallImageWidth;	
	}
	
	public int getSmallImageHeight() {
		return smallImageHeight;
	}

	public void setSmallImageHeight(int smallImageHeight) {
	    this.smallImageHeight = smallImageHeight;	
	}
	
	public String getNewPrice() {
		return newPrice;
	}
	
	public void setNewPrice(String newPrice) {
		this.newPrice = newPrice;
	}
	
	public String getUsedPrice() {
		return usedPrice;
	}

	public void setUsedPrice(String usedPrice) {
		this.usedPrice = usedPrice;
	}
	
	public String getCollectiblePrice() {
		return collectiblePrice;
	}
	
	public void setCollectiblePrice(String collectiblePrice) {
		this.collectiblePrice = collectiblePrice;
	}
	
	public String getRefurbishedPrice() {
		return refurbishedPrice;
	}
	
	public void setRefurbishedPrice(String refurbishedPrice) {
		this.refurbishedPrice = refurbishedPrice;
	}
	
}
