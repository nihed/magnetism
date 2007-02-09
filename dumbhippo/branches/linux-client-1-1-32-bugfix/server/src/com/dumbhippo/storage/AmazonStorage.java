package com.dumbhippo.storage;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonS3;
import com.dumbhippo.services.TransientServiceException;

class AmazonStorage implements Storage {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(AmazonStorage.class);
	
	private Configuration config;
	private AmazonS3 s3;
	private String bucket;
	private boolean createdBucket;
	private String repositoryName;
	
	static public boolean isS3Configured() {
		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		try {
			config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
			config.getPropertyNoDefault(HippoProperty.AMAZON_SECRET_KEY);
			config.getPropertyNoDefault(HippoProperty.AMAZON_S3_BUCKET_NAME);
		} catch (PropertyNotFoundException e) {
			logger.debug("Amazon S3 property not found: {}", e.getMessage());
			return false;
		}
		return true;
	}
	
	public AmazonStorage(String repositoryName) {
		this.repositoryName = repositoryName;
		config = EJBUtil.defaultLookup(Configuration.class);
		s3 = new AmazonS3(config.getPropertyFatalIfUnset(HippoProperty.AMAZON_ACCESS_KEY_ID).trim(),
				config.getPropertyFatalIfUnset(HippoProperty.AMAZON_SECRET_KEY).trim());
		bucket = config.getPropertyFatalIfUnset(HippoProperty.AMAZON_S3_BUCKET_NAME).trim();
		createdBucket = false;
	}
	
	private void checkBucket() throws TransientServiceException {
		if (!createdBucket) {
			logger.debug("Creating S3 bucket '{}'", bucket);
			s3.createBucket(bucket);
			createdBucket = true; // if we didn't throw
			logger.debug("Bucket created OK");
		}
	}
	
	private String getObjectName(Guid guid) {
		return repositoryName + "/" + guid.toString();
	}
	
	public long store(Guid guid, String contentType, InputStream stream, long maxSize) throws StorageException {
		try {
			// we just slurp all the bytes into memory because S3 requires an MD5 anyway, so 
			// without jumping through major hoops there is no way to avoid loading everything 
			// into memory.
			byte[] content;
			try {
				content = StreamUtils.readStreamBytes(stream, maxSize);
			} catch (IOException e) {
				throw new StorageException("Failed to read input stream", e);
			}
			store(guid, contentType, content);
			return content.length;
		} finally { 
			try {
				stream.close();
			} catch (IOException e) {
				logger.warn("Failed to close input stream", e);
			}
		}
	}
	
	public void store(Guid guid, String contentType, byte[] content) throws StorageException {
		StorageLocks.getInstance().lock(guid);
		try {
			try {
				checkBucket();
			} catch (TransientServiceException e) {
				throw new StorageException("Failed to create Amazon S3 bucket", e);
			}
			
			try {
				s3.putObject(bucket, getObjectName(guid), contentType, content);
			} catch (TransientServiceException e) {
				throw new StorageException("Failed to store " + guid + " in S3", e);
			}
			
			logger.debug("Successfully stored guid " + guid);
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}
	}

	public StoredData load(Guid guid) throws StorageException {
		StorageLocks.getInstance().lock(guid);
		try {
			try {
				return s3.getObject(bucket, getObjectName(guid));
			} catch (TransientServiceException e) {
				throw new StorageException("Failed to get object from S3", e);
			} catch (NotFoundException e) {
				throw new NotStoredException("object not in S3: " + guid, e); 
			}
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}
	}

	public void delete(Guid guid) throws StorageException {
		StorageLocks.getInstance().lock(guid);
		try {
			try {
				s3.deleteObject(bucket, getObjectName(guid));
			} catch (TransientServiceException e) {
				throw new StorageException("Failed to delete object from S3");
			}
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}		
	}
}
