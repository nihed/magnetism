package com.dumbhippo.persistence;

public enum ApplicationCategory {
	ACCESSORIES("Accessories", "Utility", "TextEditor", "!System"),
	EDUCATION("Education", "Education"),
	GAMES("Games", "Game"),
	GRAPHICS("Graphics", "Graphics"),
	INTERNET("Internet", "Network"),
	OFFICE("Office", "Office"),
	OTHER("Other"),
	PROGRAMMING("Programming", "Development"),
	SOUND_AND_VIDEO("Sound & Video", "AudioVideo", "!Settings"),
	SYSTEM_TOOLS("System", "!Screensaver");
	
	String displayName;
	String[] rawCategories;
	
	ApplicationCategory(String name, String... rawCategories) {
		this.displayName = name;
		this.rawCategories = rawCategories;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	// JSP convenience
	public String getName() {
		return name().toLowerCase();
	}
	
	public String[] getRawCategories() {
		return rawCategories;
	}
}
