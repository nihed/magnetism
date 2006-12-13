package com.dumbhippo.storage;

public class StorageFactory {
	/**
	 * Depending on server configuration, returns an appropriate new 
	 * Storage object.
	 * @return
	 */
	public static Storage newStorage() {
		return new LocalStorage("user");
	}
}
