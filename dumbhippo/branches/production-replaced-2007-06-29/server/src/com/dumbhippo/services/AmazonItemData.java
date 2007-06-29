package com.dumbhippo.services;


public interface AmazonItemData {
	
	String getASIN();
	
	String getNewPrice();
	
	String getUsedPrice();
	
	String getCollectiblePrice();
	
	String getRefurbishedPrice();
	
	String getSmallImageUrl();
	
	int getSmallImageWidth();
	
	int getSmallImageHeight();
	
}
