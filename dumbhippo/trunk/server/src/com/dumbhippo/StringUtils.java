package com.dumbhippo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

public class StringUtils {
	public static String hexEncode(byte[] strs) {
		StringWriter str = new StringWriter();
		PrintWriter out = new PrintWriter(str);
		for (int i = 0; i < strs.length; i++) {
			int first = (byte) ((strs[i] & 0xF0) >> 4);
			int second = (byte) (strs[i] & 0x0F);
			out.printf("%x%x", first, second);
		}
		
		return str.toString();
	}
	
	public static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("broken Java implementation", e);
		}
	}
	
	public static String join(List<String> strings, String separator) {
		return joinUngenericList(strings, separator);
	}
	
	public static String joinUngenericList(List strings, String separator) {
		StringBuilder builder = new StringBuilder();
		Iterator iterator = strings.iterator();
		while (iterator.hasNext()) {
			boolean hasNext = true;
			String s = (String) iterator.next();
			builder.append(s);
			if (hasNext)
				builder.append(separator);
		}
		return builder.toString();
	}
	
	public static String[] splitWords(String text) {
		text = text.trim();
		if (text.length() == 0)
			return new String[0];
		return text.split("\\s");
	}

	/**
	 * The normal String.getBytes() is broken since it uses
	 * some kind of platform-specific encoding, and getBytes(String)
	 * is broken since it throws a checked exception, thus this wrapper.
	 * 
	 * @param s a string
	 * @return bytes of string in UTF-8 encoding
	 */
	public static byte[] getBytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("No UTF-8 support in JRE, totally busted");
		}
	}
	/**
	 * Make sure the returned String is no longer than maxLength characters,
	 * appending "..." if the original String had to be truncated.
	 * 
	 * @param s a String to be truncated if necessary
	 * @param the maximum length of the resulting String
	 * @return a truncated String
	 */
	public static String truncateString(String s, int maxLength) {
		String truncatedString = s;
		if (maxLength >= 0 && (s.length() > maxLength)) {
			if (maxLength > 3) {
				truncatedString = truncatedString.substring(0, maxLength - 3);
				truncatedString += "...";	
			} else {
				truncatedString = truncatedString.substring(0, maxLength);
			}
		}
		
		return truncatedString;
	}
}
