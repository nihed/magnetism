package com.dumbhippo.services;

public class AmazonItem implements AmazonItemView {
	private String itemId;
	private String title;
	private String editorialReview;
	private String imageUrl;
	private int imageWidth;
	private int imageHeight;
	private String newPrice;
	private String usedPrice;
	private String collectiblePrice;
	private String refurbishedPrice;

	public AmazonItem() {
		imageWidth = -1;
		imageHeight = -1;
	}
	
	public AmazonItem(String itemId, String title, String editorialReview, String imageUrl,
			          int imageWidth, int imageHeight, String newPrice, String usedPrice,
			          String collectiblePrice, String refurbishedPrice) {
		this.itemId = itemId;
		this.title = title;
		this.editorialReview = editorialReview;
		this.imageUrl = imageUrl;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
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

	public String getImageUrl() {
		return imageUrl;
	}
	
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public int getImageWidth() {
		return imageWidth;
	}
	
	public void setImageWidth(int imageWidth) {
	    this.imageWidth = imageWidth;	
	}
	
	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
	    this.imageHeight = imageHeight;	
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
