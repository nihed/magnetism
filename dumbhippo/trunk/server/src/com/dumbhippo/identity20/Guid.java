package com.dumbhippo.identity20;

import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * Guid is a globally-unique identifier, designed to not
 * require coordination (i.e. you can make up the GUID without asking the server
 * for the next counter value or something).
 * 
 * This class is immutable with the advantages that entails.
 * 
 * @author hp
 * 
 */
final public class Guid implements Serializable {
	
	private static final long serialVersionUID = 1L;  

	private static java.util.Random rng1;

	private static java.util.Random rng2;

	private static final int NUM_COMPONENTS = 3;

	private static final char[] hexdigits = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	// e.g. "ff" or "3c" or whatever
	private static final int CHARS_PER_LONG = 2 * Long.SIZE / 8;

	// 2 hex digits per byte, 3 longs * 8 bytes
	public static final int STRING_LENGTH = CHARS_PER_LONG * NUM_COMPONENTS;

	transient private long[] components;

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 0L;
		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}
		public ParseException(String message) {
			super(message);
		}
	}
	
	private void writeObject(java.io.ObjectOutputStream out)
    	throws IOException {
		out.defaultWriteObject();
		
		out.writeUTF(toString());
	}
	
	private void readObject(java.io.ObjectInputStream in)
    	throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		String s = in.readUTF();
		try {
			initFromString(s);
		} catch (ParseException e) {
			throw new IOException("invalid GUID value: " + s);
		}
	}
	
	public static Guid createNew() {
		long random1, random2;
		synchronized (Guid.class) {
			if (rng1 == null) {
				/*
				 * Use two different random number generators because otherwise
				 * one random is predictable from the other and having two is
				 * pointless, right?
				 */
				rng1 = new SecureRandom();
				assert (rng2 == null);
				rng2 = new SecureRandom();
			}
			random1 = rng1.nextLong();
			random2 = rng2.nextLong();
		}
		long time = System.currentTimeMillis();

		/*
		 * sandwiching the time between two randoms should keep it from largely
		 * determining the sort order, i.e. keep random distribution
		 */
		return new Guid(random1, time, random2);
	}

	private Guid() {
		components = null;
	}
	
	/**
	 * Internal constructor for copying/creating by component
	 * 
	 */
	private Guid(long first, long second, long third) {
		components = new long[NUM_COMPONENTS];
		components[0] = first;
		components[1] = second;
		components[2] = third;
	}

	public Guid(Guid source) {
		components = source.components.clone();
	}

	private void initFromString(String string) throws ParseException {
		if (string.length() != STRING_LENGTH)
			throw new ParseException(String.format(
					"String form of GUID must have %d characters but this string has %d",
					STRING_LENGTH, string.length()));

		components = hexDecode(string);
	}
	
	public Guid(String string) throws ParseException {
		initFromString(string);
	}

	public String toString() {
		String s = hexEncode(components);
		assert (s.length() == STRING_LENGTH);
		return s;
	}

	public int hashCode() {
		int result = 17;
		for (int i = 0; i < NUM_COMPONENTS; ++i) {
			int c = (int) (components[i] ^ (components[i] >>> 32));
			result = 37 * result + c;
		}
		return result;
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Guid))
			return false;
		Guid otherGuid = (Guid) other;
		for (int i = 0; i < NUM_COMPONENTS; ++i) {
			if (components[i] != otherGuid.components[i])
				return false;
		}
		return true;
	}

	public static Set<Guid> parseStrings(Set<String> strs) throws ParseException {
		Set<Guid> guids = new HashSet<Guid>(strs.size());	
		for (String str : strs) {
			guids.add(new Guid(str));
		}
		return guids;
	}	
	
	static private void hexEncode(long value, char[] hex, int start) {
		for (int count = 0; count < Long.SIZE / 8; ++count) {
			int b = (int) (value >>> count * 8) & 0xff;

			hex[start] = hexdigits[(b >>> 4)];
			++start;
			hex[start] = hexdigits[(b & 0x0f)];
			++start;
		}
	}

	static private String hexEncode(long[] components) {

		// two hex chars per byte, e.g. "ff"
		char[] hex = new char[components.length * CHARS_PER_LONG];
		int next = 0;

		for (int i = 0; i < components.length; ++i) {
			hexEncode(components[i], hex, next);
			next += CHARS_PER_LONG;
		}
		assert (next == hex.length);
		assert (next == STRING_LENGTH);

		return new String(hex);
	}

	static private long[] hexDecode(String s) throws ParseException {
		if ((s.length() % CHARS_PER_LONG) != 0)
			throw new ParseException(
					"Bad string length passed to hexDecode");

		// two hex chars per byte, e.g. "ff"
		long[] components = new long[s.length() / CHARS_PER_LONG];
		int next = 0;

		for (int i = 0; i < components.length; ++i) {
			long value = 0;
			for (int j = 0; j < Long.SIZE / 8; ++j) {
				long b;
				try {
					b = Integer.parseInt(s.substring(next + j * 2, next + j * 2
							+ 2), 16);
					assert (b <= 0xff); // shouldn't be possible to put more in
										// 2 chars
				} catch (NumberFormatException e) {
					throw new ParseException(
							"Could not parse byte in GUID", e);
				}
				value = value | (b << (j * 8));
			}

			next += CHARS_PER_LONG;

			components[i] = value;
		}

		return components;
	}
}
