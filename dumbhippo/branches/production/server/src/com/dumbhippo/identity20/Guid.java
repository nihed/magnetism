package com.dumbhippo.identity20;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
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
 * @author hp, otaylor
 * 
 */
final public class Guid implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 0L;
		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}
		public ParseException(String message) {
			super(message);
		}
	}
	// We use 80 bits of randomness to generate our GUIDs, which
	// are simply strings. Because they occur in URLs, exotic
	// characters in our GUIDs are troublesome, so we encode
	// them *base53*. While this means that generation requires
	// big integer arithmetic, that's not a big deal, after
	// generation, we can just treat things as a 14 character
	// string.
	
	private static final int NUM_BYTES = 10;
	private static final int NUM_CHARS = 14;

    public static final int STRING_LENGTH = NUM_CHARS;

	private static final BigInteger MODULUS = new BigInteger("418195493"); // 53 ^ 5

	// We exclude all vowels but 'A' to reduce the 
	// possibility of meaningful-looking words
	private static final char[] ENCODE = {
		'A', 'B', 'C', 'D', 'F', 'G', 'H', 
		'J', 'K', 'L', 'M', 'N', 'P', 'Q', 
		'R', 'S', 'T', 'V', 'W', 'X', 'Y', 
		'Z',
	    'b', 'c', 'd', 'f', 'g', 'h', 'j', 
	    'k', 'l', 'm', 'n', 'p', 'q', 'r', 
	    's', 't', 'v', 'w', 'x', 'y', 'z',
	    '0', '1', '2', '3', '4', '5',
	    '6', '7', '8', '9'
    };

    private static java.security.SecureRandom rng;
	
    private String str;
    
    private static SecureRandom makeRng() {
		SecureRandom r = new SecureRandom();
			
        // Seed manually out of distrust of the builtin
        // facilities; GNU classPath has bad seeding and
        // the Sun implementation isn't checkable.
        byte[] seed = new byte[20];
        try {
            FileInputStream f = new FileInputStream("/dev/random");
            int numRead = 0;
            while (numRead < seed.length)
            	numRead += f.read(seed, numRead, seed.length - numRead);
            f.close();
        } catch (IOException e) {
        	throw new RuntimeException("Error reading from /dev/random");
        }	

        r.setSeed(seed);
        
        return r;
    }
    
    private static String encodeBase53(byte[] bytes) {
        // Use the signum constructor so that our input isn't construed
        // as two's complement
        BigInteger v = new BigInteger(1, bytes);

        char[] buf = new char[NUM_CHARS];

        // Chunk out 5 "digits" at a time with big integer arithmetic
        // and then use integer arithmetic to split them up; it's
        // possibly over-optimization, but it is about 4 times as fast
        // as going digit by digit.
        for (int i = 0; i < NUM_CHARS; i += 5) {
            BigInteger n[] = v.divideAndRemainder(MODULUS);
            v = n[0];
            int m = n[1].intValue();
            for (int j = 0; j < 5 && i + j < NUM_CHARS; j++) {
                buf[i + j] = ENCODE[m % 53];
                m /= 53;
            }
        }
        
        return new String(buf);
    }
    
	private static String generate() {
		if (rng == null)
			rng = makeRng();

        byte[] random = new byte[NUM_BYTES];
        rng.nextBytes(random);

        return encodeBase53(random);
    }
	
	public static void validate(String guid) throws ParseException {
        // Validation is just that everything is alphanumeric
        // and of the right length; this should guard against
		// cross-site-scripting or whatever. We actually only
		// generate a subset of these values, but don't check that.
		if (guid.length() != NUM_CHARS)
			throw new ParseException("GUID has wrong length " + guid.length() + ": " +
					guid.substring(0, Math.min(guid.length(), 10)) + " ...");
		
		for (int i = 0; i < NUM_CHARS; i++) {
			char c = guid.charAt(i);
			if (!((c >= 'A' && c <= 'Z') ||
		          (c >= 'a' && c <= 'z') ||
		          (c >= '0' && c <= '9')))
				throw new ParseException("GUID has bad characters");
		}
	}
		
	private void writeObject(java.io.ObjectOutputStream out)
    	throws IOException {
		out.defaultWriteObject();
		
		out.writeUTF(str);
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
		Guid result = new Guid();
		result.str = generate();
		
		return result;
	}
	
	private Guid() {
	}

	public Guid(Guid other) {
		str = other.str;
	}

	private void initFromString(String string) throws ParseException {
		validate(string);
		str = string;
	}
	
	public Guid(String string) throws ParseException {
		initFromString(string);
	}

	@Override
	public String toString() {
		return str;
	}

	@Override
	public int hashCode() {
		// We could just hash the first 6 characters, but
		// what we save in computation, we probably lose
		// compared to the optimization of String.hashCode()
		return str.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Guid))
			return false;
		return str.equals(((Guid)other).str);
	}

	public static Set<Guid> parseStrings(Set<String> strs) throws ParseException {
		Set<Guid> guids = new HashSet<Guid>(strs.size());	
		for (String str : strs) {
			guids.add(new Guid(str));
		}
		return guids;
	}
	
	static public Guid parseJabberId(String username) throws ParseException {
		StringBuilder transformedName = new StringBuilder();
		for (int i = 0; i < username.length(); i++) {
			if (i+1 < username.length() && username.charAt(i+1) == '_') {
				transformedName.append(Character.toLowerCase(username.charAt(i)));
				i++;
			} else {
				transformedName.append(Character.toUpperCase(username.charAt(i)));
			}
		}
		return new Guid(transformedName.toString());
	}
	
	static public Guid parseTrustedJabberId(String username) {
		try {
			return parseJabberId(username);
		} catch (ParseException e) {
			throw new RuntimeException("Jabber ID was known trusted and failed to parse: " + username, e);
		}
	}
	
	public String toJabberId(String domain) {
		StringBuilder sb = new StringBuilder();

		// Reverse the transformation in MessageGlueBean:jiveUserNameToGuid
		String id = toString();
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append(Character.toLowerCase(c));
			} else if (Character.isLowerCase(c)) {
				sb.append(c);
				sb.append('_');
			} else {
				sb.append(c);
			}
		}
			
		if (domain != null) {
			sb.append('@');
			sb.append(domain);
		}
		
		return sb.toString();
	}

}
