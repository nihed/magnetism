/**
 * 
 */
package com.dumbhippo.server;

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
final class AuthKey {
	private byte[] data;
	
	public static final AuthKey createNew() {
		Random r = new SecureRandom();
		byte[] data = new byte[10];
		r.nextBytes(data);
		
		return new AuthKey(data);
	}
	
	private AuthKey(byte[] data) {
		this.data = data; 
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
		if (!(other instanceof AuthKey))
			return false;
		AuthKey otherAuthKey = (AuthKey) other;
		if (data.length != otherAuthKey.data.length)
			return false;
		for (int i = 0; i < data.length; ++i)
			if (data[i] != otherAuthKey.data[i])
				return false;
		return true;
	}
}
