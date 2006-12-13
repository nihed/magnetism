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
	
	static public final int SHA1_HEX_LENGTH = 40;
	
	static public MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA algorithm provider missing", e);
		}
	}
	
	static public void update(MessageDigest md, String s) {
		if (s == null)
			md.update((byte) 0);
		else
			md.update(StringUtils.getBytes(s));
	}
	
	static public void update(MessageDigest md, int value) {
		while (value != 0) {
			md.update((byte) (value & 0xff));
			// >>> 0-fills vs. >> sign-bit-fills
			value = value >>> 8;
		}
	}

	static public void update(MessageDigest md, long value) {
		while (value != 0) {
			md.update((byte) (value & 0xff));
			// >>> 0-fills vs. >> sign-bit-fills
			value = value >>> 8;
		}
	}
	
	static public String digest(MessageDigest md) {
		return StringUtils.hexEncode(md.digest());
	}
	
	/**
	 * Computes a lowercase hex-encoded SHA-1 digest of the 
	 * given token prepended to the given secret.
	 * 
	 * @param token token to prepend
	 * @param secret the secret
	 * @return SHA-1 digest as hex string
	 */
	public String computeDigest(String token, String secret) {
		MessageDigest md = newDigest();
		update(md, token);
		update(md, secret);
		return digest(md);
	}
	
	/**
	 * Computes a lowercase hex-encoded SHA-1 digest of the 
	 * given data.
	 * 
	 * @param data what to digest
	 * @return SHA-1 digest as hex string
	 */
	public String computeDigest(String data) {
		MessageDigest md = newDigest();
		update(md, data);
		return digest(md);
	}
}
