package com.dumbhippo.storage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public class StorageFactory {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StorageFactory.class);
	
	static private final String REPOSITORY_NAME = "user";
	
	/**
	 * Depending on server configuration, returns an appropriate new 
	 * Storage object.
	 * @return
	 */
	public static Storage newStorage() {
		if (AmazonStorage.isS3Configured()) {
			logger.info("S3 is configured, using StackedStorage for file storage with local then amazon S3");
			return new StackedStorage(new LocalStorage(REPOSITORY_NAME), new AmazonStorage(REPOSITORY_NAME));
		} else {
			logger.info("No S3 configured, using local file storage");
			return new LocalStorage(REPOSITORY_NAME);
		}
	}
}
