package com.dumbhippo.storage;

import java.io.InputStream;

import com.dumbhippo.identity20.Guid;

public interface Storage {
	/**
	 * Reads the given stream to the end, and stores its contents keyed by the 
	 * given guid. Fails if something is already stored under this guid. 
	 * Closes the input stream when finished.
	 * 
	 * The contentType is canonically stored in our database, not in the storage backend, 
	 * but Amazon S3 can store the contentType also so we do that.
	 * 
	 * Throws TooBigException if maxSize is exceeded.
	 * 
	 * @param guid
	 * @param contentType
	 * @param stream
	 * @param maxSize
	 * @return size of the stored stream in bytes
	 * @throws StorageException
	 */
	public long store(Guid guid, String contentType, InputStream stream, long maxSize) throws StorageException;
	
	/**
	 * Stores the given byte array, same semantics as the version that takes an input stream.
	 * 
	 * @param guid
	 * @param contentType
	 * @param content
	 * @throws StorageException
	 */
	public void store(Guid guid, String contentType, byte[] content) throws StorageException;
	
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
