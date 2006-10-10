package com.dumbhippo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLUtils {
	public static String buildUrl(String base, String... parameters) {
		StringBuilder url = new StringBuilder(base);
		if (parameters.length > 0) {
			url.append("?");
		}
		for (int i = 0; i < parameters.length; i+=2) {
			if (i != 0 ) {
				url.append("&");
			}
			url.append(parameters[i]);
			url.append("=");
			if (parameters[i+1] != null) {
				try {
					url.append(URLEncoder.encode(parameters[i+1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return url.toString();
	}
}