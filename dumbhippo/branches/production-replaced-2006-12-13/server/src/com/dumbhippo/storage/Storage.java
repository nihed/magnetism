package com.dumbhippo.storage;

import java.io.InputStream;

import com.dumbhippo.identity20.Guid;

public interface Storage {
	/**
	 * Reads the given stream to the end, and stores its contents keyed by the 
	 * given guid. Fails if something is already stored under this guid. 
	 * Closes the input stream when finished.
	 * 
	 * @param guid
	 * @param stream
	 * @param maxSize
	 * @return size of the stored stream in bytes
	 * @throws StorageException
	 */
	public long store(Guid guid, InputStream stream, long maxSize) throws StorageException;
	
	/**
	 * Looks up stored data under the given guid, and returns an open stream
	 * if the data is found.
	 * 
	 * @param guid
	 * @return
	 * @throws NotStoredException if there is no data under this guid
	 * @throws StorageException if anything else goes wrong
	 */
	public StoredData load(Guid guid) throws StorageException;
	
	/**
	 * Deletes data under the given guid. If the data doesn't exist,
	 * silently succeeds. But if it exists and deletion fails, throws
	 * an exception.
	 * 
	 * @param guid
	 * @throws StorageException
	 */
	public void delete(Guid guid) throws StorageException;
}
