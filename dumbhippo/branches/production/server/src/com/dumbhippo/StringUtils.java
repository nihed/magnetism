package com.dumbhippo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

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
	
	public static String urlDecode(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("broken Java implementation", e);
		}
	}
	
	public static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("broken Java implementation", e);
		}
	} 

	/**
	 * Url-encode an email address, not encoding the "@" symbol. 
	 * This is needed because in a mailto: url, some email apps 
	 * don't unescape the mailto: url as if it were an url.
	 * 
	 * These email apps will probably also break if the email address contains 
	 * unusual characters, but the theory is that no real email address 
	 * contains those (and that if one did we really would need to the 
	 * mail app to properly unencode). We're just trying to 
	 * avoid evil html exploits here. 
	 * 
	 * Not encoding the @ also makes the mailto: url more readable in the 
	 * browser statusbar.
	 * 
	 * @param str the unencoded string
	 * @return encoded string
	 */
	public static String urlEncodeEmail(String str) {
		String encoded = urlEncode(str);
		int i = encoded.indexOf("%40");
		if (i >= 0) {
			encoded = encoded.substring(0, i) + "@" + encoded.substring(i + "%40".length());
		} else {
			throw new RuntimeException("urlEncodeEmail is only for email addresses not '" + str + "'");
		}
		return encoded;
	}
	
	public static String join(Collection<String> strings, String separator) {
		return joinUngenericList(strings, separator);
	}
	
	public static String joinUngenericList(Collection strings, String separator) {
		StringBuilder builder = new StringBuilder();
		Iterator iterator = strings.iterator();
		while (iterator.hasNext()) {
			String s = (String) iterator.next();
			builder.append(s);
			if (iterator.hasNext())
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
	
	/**
	 * Picks a random String from an array of Strings.
	 * 
	 * @param strings an array of Strings
	 * @return a random String from the array
	 */
	public static String getRandomString(String[] strings) {
		// use seconds not milliseconds, milliseconds might always be multiples of 10 or something 
		int i = (int) ((System.currentTimeMillis() / 1000) % strings.length);
		return strings[i];			
	}

	/** 
	 * Return the SHA-1 hash of the given plainText hex-encoded in a String
	 * @param plainText A String with the plain text, or null
	 * @param salt An integer containing the salt
	 * @return A String with the hex-encoded hash, or null if either input was null
	 */
	public static String secureHash(String plainText, Integer salt) {
		if (plainText == null)
			return null;
		if (salt == null)
			return null;
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException("SHA-1 algorithm unavailable", nsae);
		}
		
		// get a String representation of the salt int, possibly including a minus sign
		String saltText = salt.toString();
		
		md.update(StringUtils.getBytes(plainText));
		md.update(StringUtils.getBytes(saltText));

		byte[] hashRaw = md.digest();
		return StringUtils.hexEncode(hashRaw);
	}

	public static boolean isAlphanumeric(String s) {
		for (char c : s.toCharArray()) {
			if (!Character.isLetterOrDigit(c))
				return false;
		}
		return true;
	}
	
	public static boolean isAlphanumericOrInSet(String s, String otherCharsAllowed) {
		for (char c : s.toCharArray()) {
			if (!Character.isLetterOrDigit(c)) {
				// this is inefficient, but we assume infrequent
				boolean allowed = false;
				for (char a : otherCharsAllowed.toCharArray()) {
					if (c == a) {
						allowed = true;
						break;
					}
				}
				if (!allowed)
					return false;
			}
		}
		return true;
	}
}
