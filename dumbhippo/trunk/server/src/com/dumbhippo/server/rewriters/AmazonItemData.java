package com.dumbhippo.server.rewriters;


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
