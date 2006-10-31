package com.dumbhippo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLUtils {
	public static String buildUrl(String base, String... parameters) {
		StringBuilder url = new StringBuilder(base);
		for (int i = 0; i < parameters.length; i+=2) {
			addQueryParameter(url, parameters[i], parameters[i+1], i==0);
		}
		return url.toString();
	}
	
	private static void addQueryParameter(StringBuilder url, String name, String value, boolean first) {
		url.append(first ? '?' : '&');
		url.append(name);
		url.append("=");
		if (value != null) {
			try {
				url.append(URLEncoder.encode(value, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}		
	}

	public static void addQueryParameter(StringBuilder url, String name, String value) {
		addQueryParameter(url, name, value, url.toString().indexOf('?') < 0);
	}
	
	public static String addQueryParameter(String url, String name, String value) {
		StringBuilder newUrl = new StringBuilder(url);
		addQueryParameter(newUrl, name, value);
		return newUrl.toString();
	}
}