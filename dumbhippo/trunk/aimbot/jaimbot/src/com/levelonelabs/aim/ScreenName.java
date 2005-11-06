package com.levelonelabs.aim;


/**
 * Immutable class, don't add any setters.
 * Represents a screen name, maintaining normal and original display 
 * forms. Importantly, equals() and hashCode() are defined by the 
 * normalized form of the screen name.
 * 
 * @author hp
 */
public class ScreenName {

	private String display;
	private String normalized;
	
	public ScreenName(String display) {
		if (display == null || display.length() == 0) {
			throw new IllegalArgumentException("invalid screen name");
		}
		this.display = display;
	}
	
	public String getDisplay() {
		return display;
	}
		
	public String getNormalized() {
		if (normalized == null) {
			normalized = normalize(display);
		}
		return normalized;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ScreenName))
			return false;
		ScreenName otherName = (ScreenName) other;
		
		return getNormalized().equals(otherName.getNormalized());
	}
	
	@Override
	public int hashCode() {
		return getNormalized().hashCode();
	}
	
	@Override
	public String toString() {
		return getNormalized();
	}
	
	private static String normalize(String str) {
	    String out = "";
	    str = str.toLowerCase();
	    char[] arr = str.toCharArray();
	    for (int i = 0; i < arr.length; i++) {
	        if (arr[i] != ' ') {
	            out = out + "" + arr[i];
	        }
	    }
	
	    return out;
	}
}
