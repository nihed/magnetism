package com.dumbhippo;


public enum ExternalAccountCategory {
	MEDIA("Photos and Video"),
	LINK_SHARING("Link Sharing"),
	BLOGGING("Blogging and Microblogging"),
	CONSUMER("Consumer Sites"),	
	MUSIC("Music"),
	NOT_CATEGORIZED("Other");

	private String categoryName;
	
	private ExternalAccountCategory(String categoryName) {
		this.categoryName = categoryName;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
}