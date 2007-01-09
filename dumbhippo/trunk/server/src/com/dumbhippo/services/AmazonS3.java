package com.dumbhippo.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.StringUtils;
import com.dumbhippo.URLUtils;

public class AmazonS3 {
	static private final Logger logger = GlobalSetup.getLogger(AmazonS3.class);
	
	static private final String host = "s3.amazonaws.com";
	static private final String hostUrl = "http://" + host;
	
	private int timeoutMilliseconds;
	private String amazonAccessKeyId;
	private String amazonSecretKey;
	
	public AmazonS3(String amazonAccessKeyId, String amazonSecretKey) {
		this.timeoutMilliseconds = 1000 * 60 * 60; // we are uploading files etc. after all 
		this.amazonAccessKeyId = amazonAccessKeyId;
		this.amazonSecretKey = amazonSecretKey;
	}

	private HttpURLConnection openS3Connection(String relativeName) throws TransientServiceException {
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) URLUtils.openConnection(new URL(hostUrl + relativeName));
		} catch (MalformedURLException e) {
			throw new RuntimeException("Bad amazon url generated");
		} catch (IOException e) {
			throw new TransientServiceException("Amazon S3: " + e.getMessage(), e);
		}
		try {
			connection.setRequestMethod("PUT");
		} catch (ProtocolException e) {
			throw new RuntimeException("failed to set method PUT", e);
		}
		
		connection.setReadTimeout(timeoutMilliseconds);
		connection.setConnectTimeout(timeoutMilliseconds);
		connection.setRequestProperty("Date", FastHttpDateFormat.getCurrentDate());
		
		return connection;
	}
	
	private void doS3Put(String relativeName, String contentType, byte[] content) throws TransientServiceException {
		HttpURLConnection connection = openS3Connection(relativeName);

		try {
			connection.setRequestMethod("PUT");
		} catch (ProtocolException e) {
			throw new RuntimeException("failed to set method PUT", e);
		}
		
		connection.setDoOutput(true);
		// connection.setDoInput(true);
		
		if (content != null) {
			String md5 = Digest.computeDigestMD5Base64(content);
			connection.setRequestProperty("Content-MD5", md5);
			connection.setRequestProperty("Content-Length", Long.toString(content.length));
		}
		if (contentType != null) {
			connection.setRequestProperty("Content-Type", contentType);
		}
		
		AmazonS3Signature.signS3Connection(amazonAccessKeyId, amazonSecretKey, connection, relativeName);
		
		try {
			logger.debug("Sending S3 request to PUT {}", relativeName);
			connection.connect();
			OutputStream out = connection.getOutputStream();
			if (content != null) {
				out.write(content);
			}
			out.flush();
			out.close();
			
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();
			logger.debug("S3 request status {}: {}", responseCode, responseMessage);
			
			if (responseCode != 200) {
				try {
					InputStream in = connection.getErrorStream();
					String xml = StreamUtils.readStreamUTF8(in);
					logger.debug("Got error xml '{}'", xml);
				} catch (Exception e) {
					logger.debug("Failed to read error stream: {}", e.getMessage());
				}
				
				throw new TransientServiceException("Amazon S3 error " + responseCode + ": " + responseMessage);
			}
		} catch (IOException e) {
			logger.debug("IO error on connecting or on reading reply code: {}", e.getMessage());
			
			throw new TransientServiceException("Amazon S3: " + e.getMessage(), e);
		}
	}

	public void createBucket(String bucketName) throws TransientServiceException  {		
		doS3Put("/" + bucketName, null, null);
	}

	// important: if the object already exists, amazon will overwrite it
	public void putObject(String bucketName, String objectName, String contentType, byte[] content) throws TransientServiceException {
		doS3Put("/" + bucketName + "/" + objectName, contentType, content);
	}
	
	public static void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
				
		try {
			/*
			 // this is an example from S3 docs, which should sign as "jZNOcbfWmD/A/f3hSvVzXZjM2HU="
 
			HttpURLConnection connection = (HttpURLConnection) (new URL(hostUrl + "/quotes/nelson").openConnection());
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Content-Md5", "c8fdb181845a4ca6b8fec737b3581d76");
			connection.setRequestProperty("Content-Type", "text/html");
			connection.setRequestProperty("Date", "Thu, 17 Nov 2005 18:49:58 GMT");
			connection.setRequestProperty("X-Amz-Meta-Author", "foo@bar.com");
			connection.setRequestProperty("X-Amz-Magic", "abracadabra");

			AmazonS3Signature.signS3Connection("44CF9590006BF252F707", "OtxrzxIsfpFjA7SwPzILwy8Bw21TLhquhboDYROV",
					connection,
					"quotes", "nelson");
			  
			*/
			
			AmazonS3 s3 = new AmazonS3("", "");
			s3.createBucket("havoc-test-bucket");
			
			s3.putObject("havoc-test-bucket", "havoc-test-file", "text/plain", StringUtils.getBytes("Hello World!"));

		} catch (Exception e) {
			System.out.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}
}
