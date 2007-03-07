package com.dumbhippo.server.applications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;

public class AppinfoIcon {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AppinfoIcon.class);

	private String theme;
	private String size;
	private String path;

	private byte[] contents;
	
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
	
	public byte[] getContents() {
		return contents;
	}
	
	public void setContents(byte[] contents) {
		this.contents = contents;
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
	
	public String getQueryString() {
		StringBuilder result = new StringBuilder();
		if (theme != null) {
			result.append("?theme=");
			result.append(StringUtils.urlEncode(theme));
		}
		if (size != null) {
			if (result.length() == 0)
				result.append("?size=");
			else
				result.append("&size=");
			result.append(StringUtils.urlEncode(size));
		}
		
		return result.toString();
	}
	
	public boolean matches(String theme, String size) {
		if (!(theme == this.theme || theme != null && theme.equals(this.theme)))
			return false;

		if (!(size == this.size || size != null && size.equals(this.size)))
			return false;
		
		return true;
	}
}
