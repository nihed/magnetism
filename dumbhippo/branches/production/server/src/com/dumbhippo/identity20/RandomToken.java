/**
 * 
 */
package com.dumbhippo.identity20;

import java.security.SecureRandom;
import java.util.Random;

import com.dumbhippo.StringUtils;

/**
 * 
 * Some random bytes for authorization; instances are immutable
 * after creation.
 * 
 * @author hp
 *
 */
public final class RandomToken {
	private byte[] data;
	
	static public final int BINARY_LENGTH = 20;
	// each byte is 2 ascii chars in hex encoding
	static public final int STRING_LENGTH = BINARY_LENGTH * 2;
	
	public static final RandomToken createNew() {
		Random r = new SecureRandom();
		byte[] data = new byte[BINARY_LENGTH];
		r.nextBytes(data);
		
		return new RandomToken(data);
	}
	
	private RandomToken(byte[] data) {
		this.data = data; 
	}
	
	public byte[] getBytes() {
		return data;
	}
	
	@Override
	public String toString() {
		String ret = StringUtils.hexEncode(data);
		if (ret.length() != STRING_LENGTH)
			throw new RuntimeException("Broken: wrong string length for hex encoded random token");
		return ret;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		for (int i = 0; i < data.length; ++i) {
			int c = (int) (data[i] ^ (data[i] >>> 32));
			result = 37 * result + c;
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof RandomToken))
			return false;
		RandomToken otherAuthKey = (RandomToken) other;
		if (data.length != otherAuthKey.data.length)
			return false;
		for (int i = 0; i < data.length; ++i)
			if (data[i] != otherAuthKey.data[i])
				return false;
		return true;
	}
}
