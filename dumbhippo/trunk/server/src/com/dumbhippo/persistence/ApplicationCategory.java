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
	SOUND_AND_VIDEO("Sound & Video", "Multimedia", "!Settings"),
	SYSTEM_TOOLS("System", "!Screensaver");
	
	String name;
	String[] rawCategories;
	
	ApplicationCategory(String name, String... rawCategories) {
		this.name = name;
		this.rawCategories = rawCategories;
	}
	
	public String getName() {
		return name;
	}
	
	public String[] getRawCategories() {
		return rawCategories;
	}
}
