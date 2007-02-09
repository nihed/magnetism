package com.dumbhippo.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.dumbhippo.identity20.Guid;

/**
 * Manage locks (within this process) on stored guid; this is 
 * done separately from the storage backend because we need to lock
 * globally (across all backends) as well as between instances of the 
 * same backend.
 * 
 * FIXME this has to be clusterwide probably; though on the other hand, 
 * the only three options (get, put, delete) are more or less atomic for both 
 * the local file and the S3 backend, so locking may be pointless-ish.
 * 
 * @author Havoc Pennington
 *
 */
class StorageLocks {

	private static StorageLocks instance = new StorageLocks();
	
	public static StorageLocks getInstance() {
		return instance;
	}
	
	private Map<Guid,Lock> locks;
	
	private StorageLocks() {
		locks = new HashMap<Guid,Lock>();
	}
	
	public void lock(Guid guid) {
		Lock lock;
		
		synchronized (this) {
			lock = locks.get(guid);
			if (lock == null) {
				lock = new ReentrantLock();
				locks.put(guid, lock);
			}
		}
		
		// don't want the whole method synchronized since we don't want
		// to lock the global StorageLocks while we're blocking on this lock
		lock.lock();
	}
	
	public void unlock(Guid guid) {
		Lock lock;
		synchronized (this) {
			lock = locks.get(guid);
			if (lock == null) {
				throw new RuntimeException("Trying to unlock guid but it's not locked " + guid);
			}
		}
		lock.unlock();
	}
}
