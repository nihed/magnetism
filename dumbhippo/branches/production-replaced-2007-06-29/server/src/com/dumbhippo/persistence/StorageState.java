package com.dumbhippo.persistence;

/**
 * Tracks whether the database object has an associated data blob
 * outside the database (on the filesystem or on S3 or whatever).
 * NOT_STORED is the initial state if we haven't successfully saved,
 * STORED means we have successfully committed to storage,
 * DELETING means we are about to try deleting it, 
 * DELETED means we have successfully deleted from storage.
 * 
 * @author Havoc Pennington
 *
 */
public enum StorageState {
	NOT_STORED,
	STORED,
	DELETING,
	DELETED
}
