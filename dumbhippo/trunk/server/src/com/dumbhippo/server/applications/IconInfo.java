package com.dumbhippo.server.applications;

public class IconInfo {
	private String theme;
	private String size;
	private String path;
	
	public IconInfo(String theme, String size, String path) {
		this.theme = theme;
		this.size = size;
		this.path = path;
	}
	
	public String getTheme() {
		return theme;
	}

	public String getSize() {
		return size;
	}
	
	public String getPath() {
		return path;
	}
}
