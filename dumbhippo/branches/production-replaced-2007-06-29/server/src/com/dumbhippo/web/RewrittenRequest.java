package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RewrittenRequest extends HttpServletRequestWrapper {
	private String path;
	
	public RewrittenRequest(HttpServletRequest original, String newPath) {
		super(original);
		
		if (newPath == null)
			throw new IllegalArgumentException("null path");
		
		path = newPath;
	}
	
	@Override
	public String getRequestURI() {
		return getContextPath() + path;
	}

	@Override
	public StringBuffer getRequestURL() {
		String scheme = getScheme();
		int port = getServerPort();
		if ((scheme.equals("http") && port == 80) ||
			(scheme.equals("https") && port == 443))
			port = -1;
		
		StringBuffer result = new StringBuffer(scheme);
		result.append("://");
		result.append(getServerName());
		if (port != -1) {
			result.append(":");
			result.append(port);
		}
		result.append(getContextPath());
		result.append(path);
		String query = getQueryString();
		if (query != null) {
			result.append("?");
			result.append(query);
		}
		
		return result;
	}
	
	@Override
	public String getServletPath() {
		return path;
	}
}
