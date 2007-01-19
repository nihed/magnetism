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
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.storage.StoredData;

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
		logger.debug("created S3 service object with keyId={} and secret key length {} (should be 40?)", this.amazonAccessKeyId, this.amazonSecretKey.length());
		if (amazonSecretKey.trim() != amazonSecretKey)
			logger.warn("Amazon secret key appears to have leading/trailing whitespace");
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
	
	private int safeGetResponseCode(HttpURLConnection connection) {
		int responseCode;
		try {
			responseCode = connection.getResponseCode();
		} catch (Exception e) {
			responseCode = -1;
		}
		return responseCode;
	}
	
	private void logErrorStream(HttpURLConnection connection) {
		try {
			int responseCode = safeGetResponseCode(connection);
			
			InputStream in = connection.getErrorStream();
			String xml = StreamUtils.readStreamUTF8(in);
			logger.debug("Got error code {} xml '{}'", responseCode, xml);
		} catch (Exception e) {
			logger.debug("Failed to read error stream: {}", e.getMessage());
		}
	}
	
	private void doS3Put(String relativeName, String contentType, byte[] content) throws TransientServiceException {
	
		logger.debug("Sending PUT request to S3 for {}", relativeName);
		
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
				logErrorStream(connection);
				throw new TransientServiceException("Amazon S3 error " + responseCode + ": " + responseMessage);
			}
		} catch (IOException e) {
			logger.debug("IO error on connecting or on reading reply code: {}", e.getMessage());
			
			logErrorStream(connection);
			
			throw new TransientServiceException("Amazon S3: " + e.getMessage(), e);
		}
	}

	public void createBucket(String bucketName) throws TransientServiceException  {
		
		doS3Put("/" + bucketName, null, null);
	}

	// important: if the object already exists, amazon will overwrite it
	// you might think a streaming API here would be nice, but since we have to md5 the content before writing
	// it, it's quite complicated to do this without reading into memory. I'm pretty sure we're copying the 
	// whole file about a billion times anyway due to how various java apis we're using work.
	public void putObject(String bucketName, String objectName, String contentType, byte[] content) throws TransientServiceException {
		doS3Put("/" + bucketName + "/" + objectName, contentType, content);
	}
	
	public StoredData getObject(String bucketName, String objectName) throws TransientServiceException, NotFoundException {
		String relativeName = "/" + bucketName + "/" + objectName;
		
		logger.debug("Sending GET request to S3 for {}", relativeName);
		
		HttpURLConnection connection = openS3Connection(relativeName);
		
		try {
			connection.setRequestMethod("GET");
		} catch (ProtocolException e) {
			throw new RuntimeException("failed to set method GET", e);
		}
		
		connection.setDoInput(true);
		
		// Java will add this automatically, but too late for signS3Connection to include it in the signature.
		// amazon does not care about it, but it is harmless. 
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		connection.setRequestProperty("Content-Length", "0"); // amazon seems to want this for some reason
		
		AmazonS3Signature.signS3Connection(amazonAccessKeyId, amazonSecretKey, connection, relativeName);
		
		try {
			connection.connect();
			
			StoredData sd = new StoredData(connection.getInputStream(), connection.getContentLength());
			sd.setMimeType(connection.getContentType());
			
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();
			logger.debug("S3 request status {}: {}", responseCode, responseMessage);
			
			if (responseCode == 404) {
				throw new NotFoundException("Object '" + relativeName + "' not found on Amazon S3");
			} else if (responseCode != 200) {
				logErrorStream(connection);
				
				throw new TransientServiceException("Amazon S3 error " + responseCode + ": " + responseMessage);
			}
			
			return sd;
		} catch (IOException e) {
			logger.debug("IO error on connecting or on reading reply code: {}", e.getMessage());
			
			// IOException can mean an actual IO error, or it can mean certain non-success response codes.
			// The codes that trigger exceptions seem to vary with http method. Not sure how to deal with 
			// this, so 404 is handled both here and up above.
			if (safeGetResponseCode(connection) == 404) {
				throw new NotFoundException("Object '" + relativeName + "' not found on Amazon S3");
			}
			
			logErrorStream(connection);
			
			throw new TransientServiceException("Amazon S3: " + e.getMessage(), e);			
		}
	}
	
	private void doS3Delete(String relativeName) throws TransientServiceException {
		logger.debug("Sending DELETE request to S3 for {}", relativeName);
		
		HttpURLConnection connection = openS3Connection(relativeName);
		
		try {
			connection.setRequestMethod("DELETE");
		} catch (ProtocolException e) {
			throw new RuntimeException("failed to set method DELETE", e);
		}		
		
		// Java will add this automatically, but too late for signS3Connection to include it in the signature.
		// amazon does not care about it, but it is harmless. 
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		AmazonS3Signature.signS3Connection(amazonAccessKeyId, amazonSecretKey, connection, relativeName);
		
		try {
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();
			logger.debug("S3 request status {}: {}", responseCode, responseMessage);
			
			// note 204 No Content is success for delete, not 200 OK
			if (responseCode != 204) {
				logErrorStream(connection);
				
				throw new TransientServiceException("Amazon S3 error " + responseCode + ": " + responseMessage);
			}
		} catch (IOException e) {
			logger.debug("IO error on connecting or on reading reply code: {}", e.getMessage());
			
			logErrorStream(connection);
			
			throw new TransientServiceException("Amazon S3: " + e.getMessage(), e);			
		}		
	}
	
	public void deleteObject(String bucketName, String objectName) throws TransientServiceException {
		String relativeName = "/" + bucketName + "/" + objectName;
		doS3Delete(relativeName);
	}
	
	public void deleteBucket(String bucketName) throws TransientServiceException {
		String relativeName = "/" + bucketName;
		doS3Delete(relativeName);
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

			StoredData sd = s3.getObject("havoc-test-bucket", "havoc-test-file");
			logger.debug("Retrieved object with type {} length {}", sd.getMimeType(), sd.getSizeInBytes());
			
			s3.deleteObject("havoc-test-bucket", "havoc-test-file");
			
			try {
				sd = s3.getObject("havoc-test-bucket", "havoc-test-file");
				logger.error("Retrieved object with type {} length {}", sd.getMimeType(), sd.getSizeInBytes());
			} catch (NotFoundException e) {
				logger.debug("Deleted object no longer exists, yay");
			}
			
			s3.deleteBucket("havoc-test-bucket");
			
		} catch (Exception e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage());
		}
	}
}
