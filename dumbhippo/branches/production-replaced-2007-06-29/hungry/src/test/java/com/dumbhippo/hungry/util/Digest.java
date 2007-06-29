/**
 * This is cut-and-pasted from com.dumbhippo since we don't depend on that
 */
package com.dumbhippo.hungry.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author hp
 *
 */
public class Digest {
	private static MessageDigest messageDigest;
	
	static private String hexEncode(byte[] strs) {
		StringWriter str = new StringWriter();
		PrintWriter out = new PrintWriter(str);
		for (int i = 0; i < strs.length; i++) {
			int first = (byte) ((strs[i] & 0xF0) >> 4);
			int second = (byte) (strs[i] & 0x0F);
			out.printf("%x%x", first, second);
		}
		
		return str.toString();
	}
	
	static private synchronized String cachedComputeDigest(String token, String secret) {
		try {
			messageDigest = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new IllegalStateException("SHA algorithm provider missing", e);
		}
		
	    messageDigest.update(token.getBytes());
		byte[] expectedDigest = messageDigest.digest(secret.getBytes());
		 
        return hexEncode(expectedDigest);
	}
	
	/**
	 * Computes a lowercase hex-encoded SHA-1 digest of the 
	 * given token prepended to the given secret.
	 * 
	 * Right now uses a global MessageDigest, but the idea is that 
	 * we could tune this method to use various caching strategies for
	 * the MessageDigest if appropriate.
	 * 
	 * @param token token to prepend
	 * @param secret the secret
	 * @return SHA-1 digest as hex string
	 */
	public String computeDigest(String token, String secret) {
		return cachedComputeDigest(token, secret);
	}
}
