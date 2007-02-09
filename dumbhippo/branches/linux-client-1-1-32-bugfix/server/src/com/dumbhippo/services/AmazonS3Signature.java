package com.dumbhippo.services;

import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;

import com.dumbhippo.Base64;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;


/** This code is from the S3 docs, 
 * http://docs.amazonwebservices.com/AmazonS3/2006-03-01/gsg/
 *
 */
public class AmazonS3Signature {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonS3Signature.class);
	
	static private final String AMAZON_HEADER_PREFIX = "x-amz-";
	static private final String ALTERNATIVE_DATE_HEADER = "x-amz-date";

	// resourceKey can be null if it's just a bucket
	static public void signS3Connection(String awsAccessKey, String secretAccessKey,
			HttpURLConnection connection, String resource) { 
		
		StringBuilder canonicalBuffer = new StringBuilder();
		canonicalBuffer.append(connection.getRequestMethod() + "\n");

		Map<String,List<String>> headers = connection.getRequestProperties();
		
		// Add all interesting headers to a list, then sort them.  "Interesting"
		// is defined as Content-MD5, Content-Type, Date, and x-amz-
		SortedMap<String,String> interestingHeaders = new TreeMap<String,String>();
		if (headers != null) {
		    for (String header : headers.keySet()) {
		        if (header == null) // possible? amazon sample code checks it for some reason
		        	continue;
		        String lowerHeader = header.toLowerCase();

		        // Ignore any headers that are not particularly interesting.
		        if (lowerHeader.equals("content-type") || lowerHeader.equals("content-md5") || lowerHeader.equals("date") ||
		            lowerHeader.startsWith(AMAZON_HEADER_PREFIX))
		        {
		            List<String> values = headers.get(header);
		            interestingHeaders.put(lowerHeader, listToCommaString(values));
		        }
		    }
		}

		if (interestingHeaders.containsKey(ALTERNATIVE_DATE_HEADER)) {
		    interestingHeaders.put("date", "");
		}

		// these headers require that we still put a new line in after them,
		// even if they don't exist.
		if (!interestingHeaders.containsKey("content-type")) {
		    interestingHeaders.put("content-type", "");
		}
		if (!interestingHeaders.containsKey("content-md5")) {
		    interestingHeaders.put("content-md5", "");
		}

		// Finally, add all the interesting headers (i.e.: all that startwith
		// x-amz- ;-))
		for (String header : interestingHeaders.keySet()) {
		    if (header.startsWith(AMAZON_HEADER_PREFIX)) {
		        canonicalBuffer.append(header).append(':').append(interestingHeaders.get(header));
		    } else {
		        canonicalBuffer.append(interestingHeaders.get(header));
		    }
		    canonicalBuffer.append("\n");
		}

		// don't include the query parameters...
		int queryIndex = resource.indexOf('?');
		if (queryIndex == -1) {
		    canonicalBuffer.append(resource);
		} else {
		    canonicalBuffer.append(resource.substring(0, queryIndex));
		}

		//...unless there is an acl or torrent parameter
		if (resource.matches(".*[&?]acl($|=|&).*")) {
		    canonicalBuffer.append("?acl");
		} else if (resource.matches(".*[&?]torrent($|=|&).*")) {
		    canonicalBuffer.append("?torrent");
		}
		
		//Acquire an HMAC/SHA1 from the raw key bytes.
		SecretKeySpec signingKey =
		    new SecretKeySpec(StringUtils.getBytes(secretAccessKey), "HmacSHA1");

		// Acquire the MAC instance and initialize with the signing key.
		Mac mac = null;
		try {
		    mac = Mac.getInstance("HmacSHA1");
		} catch (NoSuchAlgorithmException e) {
		    // should not happen
		    throw new RuntimeException("Could not find sha1 algorithm", e);
		}
		try {
		    mac.init(signingKey);
		} catch (InvalidKeyException e) {
		    // also should not happen
		    throw new RuntimeException("Could not initialize the MAC algorithm", e);
		}

		// Compute the HMAC on the digest, and set it.
		String canonicalString = canonicalBuffer.toString();
		String headerSignature = Base64.encode(mac.doFinal(canonicalString.getBytes()));

		//logger.debug("will sign '{}' as bytes '{}'", canonicalString, StringUtils.hexEncode(StringUtils.getBytes(canonicalString)));
		//logger.debug("signature '{}'", headerSignature);
		
		// set the Authorization header with the value we've just calculated.
		connection.setRequestProperty("Authorization", "AWS " + awsAccessKey + ":" + headerSignature);
	}
	
	// helper function converts list to comma-separated string
	static private String listToCommaString(List<String> values) {
	    StringBuilder buf = new StringBuilder();
	    for (String s : values) {
	        buf.append(s.replaceAll("\n", "").trim());
	        buf.append(",");
	    }
	    // chop trailing comma
	    if (buf.length() > 0)
	    	buf.setLength(buf.length() - 1); 
	    return buf.toString();
	}	
}
