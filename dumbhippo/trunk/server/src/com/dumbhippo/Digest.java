/**
 * 
 */
package com.dumbhippo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author hp
 *
 */
public class Digest {
	private static MessageDigest messageDigest;
	
	static private synchronized String cachedComputeDigest(String token, String secret) {
		try {
			messageDigest = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new IllegalStateException("SHA algorithm provider missing", e);
		}
		
	    messageDigest.update(token.getBytes());
		byte[] expectedDigest = messageDigest.digest(secret.getBytes());
		 
        return StringUtils.hexEncode(expectedDigest);
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
