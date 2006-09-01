package com.dumbhippo.live;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;

/**
 * This class maintains several possible references to a LiveObject.
 * First, an object can be explicitly strongly referenced; e.g. for
 * a LiveUser, we strongly reference users which are currently present.
 * Second, there is a cache of recently touched objects which can 
 * be updated via the touch method.
*/
class LiveObjectCache<T extends LiveObject> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveObject.class);
	
	private int maxAge;
	LiveObjectFactory<T> factory;
	
	private static class CacheEntry<T extends LiveObject> {
		private Guid guid;
		private T t;
		private boolean inUpdate;
		int strongCount;
		int cacheAge;
		
		CacheEntry(Guid guid) {
			this.guid = guid;
			inUpdate = true;
			strongCount = 0;
			cacheAge = 0;
		}
		
		private void waitForUpdate() {
			if (!inUpdate)
				return;
			
			if (LiveState.verboseLogging)
				logger.debug("Waiting for CacheEntry {} to be finished updating", guid);
			
			while (inUpdate) {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while waiting for cache update");
				}
			}
			
			if (LiveState.verboseLogging)
				logger.debug("CacheEntry {} finished updating", guid);
		}
		
		public synchronized T get() {
			waitForUpdate();
			
			if (!isValid()) {
				// this can only happen if we were waiting for another thread to update the entry
				// and that thread threw an exception 
				// we are not going to coordinate the waiting threads to have one of them retry 
				// creating the object for the entry, but will have all of them throw this exception
				// we will retry creating the object for this entry next time it is requested
				throw new RuntimeException(LiveState.objectCreationFailedText + " Guid: " + guid);	
			}
			
			return t;
		}
		
		public synchronized T getForUpdate() {
			waitForUpdate();

			if (!isValid()) {
				// see the comment in get() above
				throw new RuntimeException(LiveState.objectCreationFailedText + " Guid: " + guid);	
			}
			
			inUpdate = true;			
			return t;
		}

		public synchronized void markInvalidEntryForUpdate() {
			if (isValid()) {
				throw new RuntimeException("markInvalidEntryForUpdate should not be called for a valid entry");
			}
		
			inUpdate = true;
		}
		
		public synchronized void update(T t) {
			if (!inUpdate) {
				logger.warn("Attempt to update an entry not primed for update");
				return;
			}
			
			if (LiveState.verboseLogging)
				logger.debug("Finished update for {}", guid);
			
			// we need to indicate that the thread is done updating the object
			// to store in the cache, regardless of whether the thread was able
			// to get a valid object or threw an exception
			inUpdate = false;
			// in we got a null object, we leave the old data here because it is better if 
			// there is a permanent database failure when getting the object
			// however, if we expect most failures to be transient, we could set the value 
			// to null here for it to have a chance to be updated correctly next time around
			// this approach should also include changing the callers to not pass in the old
			// value when an exception occurs, but to pass in null
			if (t != null) {
				this.t = t;
			}
			notifyAll();
		}

		public synchronized T peek() {
			return t;
		}
		
		public synchronized boolean isInUpdate() {
			return inUpdate;
		}
		
		public synchronized boolean isValid() {
			return (t != null) || inUpdate;
		}

	}
	
	private HashMap<Guid, CacheEntry<T>> entries;

	/**
	 * Create a new object cache
	 * @param factory used to create an object to return on cache miss
	 * @param maxAge number of times age() can be called without a
	 *    call to get() before the object is discarded.
	 */
	public LiveObjectCache(LiveObjectFactory<T> factory, int maxAge) {
		this.factory = factory;
		this.maxAge = maxAge;
		entries = new HashMap<Guid, CacheEntry<T>>();
	}
	
	/**
	 * Will always return a valid entry or throw a runtime exception.
	 * 
	 * @param guid
	 * @return a cache entry
	 */
	private CacheEntry<T> getEntry(Guid guid) {
		CacheEntry<T> entry;
		boolean needNew = false;

		synchronized(this) {
			 entry = entries.get(guid);
			 if (entry == null) {
				 entry = new CacheEntry<T>(guid);
				 entries.put(guid, entry);
				 needNew = true;
			 } else if (!entry.isValid()) {
                 // our previous attempt to create an entry failed, so let's retry
				 if (LiveState.verboseLogging)
					logger.debug("Entry for {} is not valid, will retry", guid);
				 // once we mark an invalid entry for update, isValid() will be returning true, 
				 // so other threads will bypass this block and wait for us to complete 
				 entry.markInvalidEntryForUpdate();
				 needNew = true;
			 }
			 entry.cacheAge = 0;
		}
		
		if (needNew) {
			T t = null;
			try {
				t = factory.create(guid);			    
				if (t == null) {					
					// this ensures that we never return an invalid entry, factory create methods
					// must never return null anyway
					throw new RuntimeException("Factory create method returned null for Guid " + guid);
				}			
				
				 if (LiveState.getThrowRandomExceptionOnEntryCreation() && Math.random() > 0.5)
					 throw new RuntimeException(LiveState.testExceptionText + " Guid: " + guid);
				 
			} finally {
				entry.update(t);
			}
		} else if (LiveState.verboseLogging) {
				logger.debug("Entry for {} was already in the cache", guid);
		}
		
		return entry;
	}
	
	/**
	 * Get an object from the cache; if the object doesn't exist, it
	 * will be created using the factory passed to the constructor.
	 * 
	 * The cache age of the object is set to zero.
	 * 
	 * @param guid guid to look up in the cache
	 * @return the found or newly created object
	 */
	public T get(Guid guid) {
		CacheEntry<T> entry = getEntry(guid);
		
		return entry.get();
	}

	/**
	 * Get an object from the cache; if the object doesn't exist, it
	 * will be created using the factory passed to the constructor.
	 * After the object is created or looked up, it will be marked
	 * as needing update. You must copy the result then call update()
	 * passing in the newly copied object. If you fail to call update(),
	 * then anyone trying to access the object will hang forever.
	 * 
	 * The cache age of the object is set to zero.
	 * 
	 * @param guid guid to look up in the cache
	 * @return the found or newly created object
	 */
	public T getForUpdate(Guid guid) {
		CacheEntry<T> entry = getEntry(guid);

		return entry.getForUpdate();
	}
	
	private synchronized CacheEntry<T> peekEntry(Guid guid) {
		CacheEntry<T> entry = entries.get(guid);	
		if ((entry == null) || !entry.isValid())
			return null;
		
		return entry;
	}

	/**
	 * Get an object from the cache if it exists and has a value.
	 * Does not reset the cache age of the object. Does not wait
	 * for creation or pending updates, but returns null if the
	 * object is currently being created and a stale object if 
	 * it is currently being updated.
	 * 
	 * @param guid guid to look up in the cache
	 * @return the found object or null
	 */
	public T peek(Guid guid) {
		CacheEntry<T> entry = peekEntry(guid);
		if (entry == null)
			return null;
		
		return entry.peek();
	}
	
	/**
	 * Get an object from the cache; if the object doesn't exist, 
	 * null will be returned. On succesful lookup, the object will be marked
	 * as needing update. You must copy the result then call update()
	 * passing in the newly copied object. If you fail to call update(),
	 * then anyone trying to access the object will hang forever.
	 * @param guid guid to look up in the cache
	 * @return the found or newly created object
	 */
	public T peekForUpdate(Guid guid) {
		CacheEntry<T> entry = peekEntry(guid);
		if (entry == null)
			return null;
		
		return entry.getForUpdate();
	}

	/**
	 * Insert a modified LiveObject into the cache; the caller must have previously
	 * either called peekForUpdate() or getForUpdate()    
	 */
	public void update(T obj) {
		CacheEntry<T> entry = entries.get(obj.getGuid());
		if (entry == null) {
			logger.warn("Attempt to update an entry not primed for update");
			return;
		}
		
		entry.update(obj);
	}		
	
	/**
	 * Adds an explicit "strong" reference to a LiveObject;
	 * the object will not be evicted until dropStrongReference
	 * is invoked.
	 * 
	 * @param obj
	 */
	public synchronized void addStrongReference(T obj) {
		CacheEntry<T> entry = entries.get(obj.getGuid());
		entry.strongCount++;
	}
	
	/**
	 * Records a LiveObject as being a candidate for eviction,
	 * assuming it has not been recently accessed.
	 * 
	 * @param obj
	 */
	public synchronized void dropStrongReference(T obj) {
		CacheEntry<T> entry = entries.get(obj.getGuid());
		entry.strongCount--;
	}

	/**
	 * Return all objects (or only strong objects) from the cache
	 * @param strongOnly if true, only return strongly referenced objects
	 * @return a set of the objects currently in the cache
	 */
	public synchronized Set<T> getAllObjects(boolean strongOnly) {
		Set<T> result = new HashSet<T>();
		
		for (CacheEntry<T> entry : entries.values()) {
			if (!strongOnly || entry.strongCount > 0) {
				T t = entry.peek();
				if (t != null) {
					result.add(t);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Count the number of objects in the cache
	 * @param strongOnly if true, only count strongly referenced objects
	 * @return the number of cached objects (this number is approximate;
	 *   cache entries that are in the process of being created will be
	 *   counted)
	 */
	public synchronized int getObjectCount(boolean strongOnly) {
		if (strongOnly) {
			int count = 0;
			
			for (CacheEntry<T> entry : entries.values()) {
				if (entry.strongCount > 0)
					count++;
			}
			
			return count;
		} else{
			return entries.size();
		}
	}
	
	/**
	 * Increase the age of all items; discard any that have been aged more maxAge times
	 * @param maxAge how many times to bump the age before discarding
	 */
	public synchronized void age() {
		for (Iterator<CacheEntry<T>> i = entries.values().iterator(); i.hasNext();) {
			CacheEntry<T> entry = i.next();
			entry.cacheAge++;
			if (entry.cacheAge > maxAge && !entry.isInUpdate() && entry.strongCount == 0) {
				T t = entry.peek();
				if (t == null) {
					logger.debug("Discarding timed-out entry with null object");
				} else {
				    logger.debug("Discarding timed-out instance of " + t.getClass().getName());
				}
				i.remove();
			}
		}
	}
	
	public synchronized void removeAllWeak() {
		for (Iterator<CacheEntry<T>> i = entries.values().iterator(); i.hasNext();) {
			CacheEntry<T> entry = i.next();
			if (!entry.isInUpdate() && entry.strongCount == 0) {
				i.remove();
			}
		}
	}
}
