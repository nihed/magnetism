package com.dumbhippo.server.applications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class AppinfoIcon {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AppinfoIcon.class);

	private String theme;
	private String size;
	private String path;
	
	public AppinfoIcon(String theme, String size, String path) {
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
	
	Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x\\1");
	
	public int getNominalSize() {
		if (size == null)
			return -1;
		
		Matcher m = SIZE_PATTERN.matcher(size);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		} else {
			return -1;
		}
	}
}
