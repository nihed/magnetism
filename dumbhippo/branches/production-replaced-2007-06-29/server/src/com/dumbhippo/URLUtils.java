package com.dumbhippo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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
	
	/** A wrapper for openConnection that does some global Mugshot defaults */
	public static URLConnection openConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		// Set long timeouts by default; most places we use a connection we'll shorten this, 
		// but the Java default is infinite I think so just fix that up
		connection.setConnectTimeout(1000 * 60 * 7);
		connection.setReadTimeout(1000 * 60 * 7);
		// this may be the default anyway
		connection.setAllowUserInteraction(false);
		// set a user agent
		if (connection instanceof HttpURLConnection) {
			//HttpURLConnection httpConnection = (HttpURLConnection) connection;
			connection.setRequestProperty("User-Agent", "Mugshot/1.0 (+http://mugshot.org/)");
		}
		return connection;
	}
}
