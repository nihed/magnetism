package com.dumbhippo.storage;


public class StorageFactory {
	
	static private final String REPOSITORY_NAME = "user";
	
	/**
	 * Depending on server configuration, returns an appropriate new 
	 * Storage object.
	 * @return
	 */
	public static Storage newStorage() {
		if (AmazonStorage.isS3Configured()) {
			return new StackedStorage(new LocalStorage(REPOSITORY_NAME), new AmazonStorage(REPOSITORY_NAME));
		} else {
			return new LocalStorage(REPOSITORY_NAME);
		}
	}
}
