//
// typica - A client library for Amazon Web Services
// Copyright (C) 2007 Xerox Corporation
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

// Modified to be one utility class, not dependent on JAXB

package com.dumbhippo.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.base64.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

public class AWSRequestUtil {
	// this is the number of automatic retries
	private static String userAgent = "typica";
	
    private static final String DateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static SimpleDateFormat httpDateFormat = new SimpleDateFormat(DateFormat, Locale.US);
    static {
    	httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    /**
     * Make a http request and process the response. This method also performs automatic retries.
	 *
     * @param method The HTTP method to use (GET, POST, DELETE, etc)
     * @param action the name of the action for this query request
     * @param params map of request params
     */
	public static void setupMethod(PostMethod method, String awsAccessKeyId, String awsSecretKey, 
			                  String action, Map<String, String> params, Map<String,List<String>> headers)
		throws HttpException, IOException {

		// add auth params, and protocol specific headers
		Map<String, String> qParams = new HashMap<String, String>(params);
		qParams.put("Action", action);
		qParams.put("AWSAccessKeyId", awsAccessKeyId);
		qParams.put("SignatureVersion", "1");
		qParams.put("Timestamp", httpDateFormat.format(new Date()));
        if (headers != null) {
            for (Iterator<String> i = headers.keySet().iterator(); i.hasNext(); ) {
                String key = i.next();
                for (Iterator<String> j = headers.get(key).iterator(); j.hasNext(); ) {
					qParams.put(key, j.next());
                }
            }
        }
		// sort params by key
		ArrayList<String> signedKeys = new ArrayList<String>(qParams.keySet());
		Collator stringCollator = Collator.getInstance();
		stringCollator.setStrength(Collator.PRIMARY);
		Collections.sort(signedKeys, stringCollator);

		// build param string
		StringBuilder resource = new StringBuilder();
		for (String key : signedKeys) {
			resource.append(key);
			resource.append(qParams.get(key));
		}
		// calculate signature
        String sig = sha1Base64(awsSecretKey, resource.toString(), false);
        qParams.put("Signature", sig);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (String key: qParams.keySet()) {
        	pairs.add(new NameValuePair(key, qParams.get(key)));
        }
		method.setRequestBody((NameValuePair[]) pairs.toArray(new NameValuePair[0]));
		method.setRequestHeader(new Header("User-Agent", userAgent));
	}
	
    protected static String urlencode(String unencoded) {
        try {
            return URLEncoder.encode(unencoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException("Could not url encode to UTF-8", e);
        }
    }	
	
    /**
     * Calculate the HMAC/SHA1 on a string.
     * @param data Data to sign
     * @param passcode Passcode to sign it with
     * @return Signature
     * @throws NoSuchAlgorithmException If the algorithm does not exist.  Unlikely
     * @throws InvalidKeyException If the key is invalid.
     */
    protected static String sha1Base64(String awsSecretAccessKey, String canonicalString,
                                          boolean urlencode)
    {
        // The following HMAC/SHA1 code for the signature is taken from the
        // AWS Platform's implementation of RFC2104 (amazon.webservices.common.Signature)
        //
        // Acquire an HMAC/SHA1 from the raw key bytes.
        SecretKeySpec signingKey =
            new SecretKeySpec(awsSecretAccessKey.getBytes(), "HmacSHA1");

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
        String b64 = new String(Base64.encode(mac.doFinal(canonicalString.getBytes())));

        if (urlencode) {
            return urlencode(b64);
        } else {
            return b64;
        }
    }	
}
