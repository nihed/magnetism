package com.dumbhippo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.identity20.Guid;

/** 
 * This is a simple stack of storage backends, designed for now to store everything in both 
 * S3 and local files. I think a better setup would be to only store in local files right away, then have 
 * cron-job-type S3 backup in a periodic task, and fetch from S3 if a file is missing locally (so once 
 * backed up, the local file is just a cache).
 * 
 * @author Havoc Pennington
 *
 */
public class StackedStorage implements Storage {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(StackedStorage.class);	
	
	private List<Storage> storages;
	
	/** 
	 * Storages should be ordered from most local to most remote; we'll prefer to load
	 * from them in order.
	 * @param storages
	 */
	public StackedStorage(Storage... storages) {
		this.storages = new ArrayList<Storage>();
		for (Storage s : storages) {
			this.storages.add(s);
		}
	}
	
	public long store(Guid guid, String contentType, InputStream stream, long maxSize) throws StorageException {
		try {
			// Since we are storing in multiple backends, the stream doesn't really work; we 
			// have to slurp into memory. (unless we got complicated and did some kind of "tee" 
			// stream)
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
			// FIXME this isn't atomic, the first one to fail just results
			// in throwing StorageException and then the file is already stored in the 
			// previous spot. To make it atomic we need some kind of prepare/commit/rollback
			// setup.
			for (Storage s : storages) {
				s.store(guid, contentType, content);
			}
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}
	}

	public StoredData load(Guid guid) throws StorageException {
		StorageLocks.getInstance().lock(guid);
		try {
			int i = 0;
			for (Storage s : storages) {
				try {
					i += 1;
					StoredData sd = s.load(guid);
					return sd;
				} catch (StorageException e) {
					// throw only on the last one
					if (i == storages.size()) {
						logger.debug("Failed to load {} from last storage in stack", guid);
						throw e;
					} else {
						logger.debug("Failed to load {} from one storage location, trying next one. {}",
								e.getClass().getName() + ": " + e.getMessage());
					}
				}
			}
			throw new NotStoredException("Failed to load file");
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}		
	}

	public void delete(Guid guid) throws StorageException {
		StorageLocks.getInstance().lock(guid);
		try {
			StorageException lastException = null;
			// FIXME this isn't atomic, we try to remove from all of them, 
			// if one fails we throw the exception.
			for (Storage s : storages) {
				try {
					s.delete(guid);
				} catch (StorageException e) {
					lastException = e;
				}
			}
			if (lastException != null)
				throw lastException;
		} finally {
			StorageLocks.getInstance().unlock(guid);
		}		
	}
}
