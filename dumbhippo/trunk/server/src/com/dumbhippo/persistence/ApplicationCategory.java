package com.dumbhippo.persistence;

import java.util.Set;

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
	SYSTEM_TOOLS("System", "System", "!Screensaver");
	
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
	
	// Returns a single raw category that, by itself, will map to this catetgory
	// will return null for the OTHER category
	public String getSingleRawCategory() {
		if (rawCategories.length > 0)
			return rawCategories[0];
		else
			return null;
	}
	
	public static ApplicationCategory fromRaw(Set<String> rawCategories) {
		for (ApplicationCategory category : values()) {
			boolean found = false;
			boolean foundNot = false;
			
			for (String rc : category.getRawCategories()) {
				if (rc.charAt(0) == '!' && rawCategories.contains(rc.substring(1)))
					foundNot = true;
				else if (rawCategories.contains(rc))
					found = true;
			}
			
			if (found && !foundNot)
				return category;
		}
		
		return ApplicationCategory.OTHER;
	}
}
