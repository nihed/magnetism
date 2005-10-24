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
	
	static public final int LENGTH = 10;
	
	public static final RandomToken createNew() {
		Random r = new SecureRandom();
		byte[] data = new byte[LENGTH];
		r.nextBytes(data);
		
		return new RandomToken(data);
	}
	
	private RandomToken(byte[] data) {
		this.data = data; 
	}
	
	public byte[] getBytes() {
		return data;
	}
	
	public String toString() {
		return StringUtils.hexEncode(data);
	}
	
	public int hashCode() {
		int result = 17;
		for (int i = 0; i < data.length; ++i) {
			int c = (int) (data[i] ^ (data[i] >>> 32));
			result = 37 * result + c;
		}
		return result;
	}

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
