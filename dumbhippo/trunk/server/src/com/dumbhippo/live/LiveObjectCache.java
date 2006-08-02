package com.dumbhippo.live;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.identity20.Guid;

/**
 * This class maintains several possible references to a LiveObject.
 * First, an object can be explicitly strongly referenced; e.g. for
 * a LiveUser, we strongly reference users which are currently present.
 * Second, there is a cache of recently touched objects which can 
 * be updated via the touch method.
 * Finally, a pure memory cache of the objects is maintained.  This
 * may be cleared at any time by the JVM if the object is neither
 * explicitly strongly referenced nor recently touched.
*/
class LiveObjectCache<T extends LiveObject> {
	private static class GuidReference<T> extends WeakReference<T> {
		private Guid guid;
		
		GuidReference(T t, Guid guid) {
			super(t);
			this.guid = guid;
		}
	}
		
	private HashMap<Guid, T> strongReferences;
	private HashMap<Guid, T> recentReferences;
	private HashMap<Guid, GuidReference<T>> weakReferences;
	private ReferenceQueue<T> queue;
	
	public LiveObjectCache() {
		strongReferences = new HashMap<Guid, T>();
		recentReferences = new HashMap<Guid, T>();			
		weakReferences = new HashMap<Guid, GuidReference<T>>();
		queue = new ReferenceQueue<T>();
	}
	
	/**
	 * Add a LiveObject to the weak cache.
	 * 
	 * @param t object to be inserted
	 */
	public void poke(T t) {
		Guid guid = t.getGuid();			
		GuidReference<T> reference = new GuidReference<T>(t, guid);
		weakReferences.put(guid, reference);
	}
	
	/**
	 * Look for a LiveObject in the weak cache.
	 * 
	 * @param guid Guid whose associated owner to check for in the weak cache
	 * @return associated LiveObject, or null
	 */
	public T peek(Guid guid) {
		GuidReference<T> reference = weakReferences.get(guid);
		if (reference != null)
			return reference.get();
		else
			return null;			
	}
	
	/**
	 * Records a LiveObject as having been recently accessed.
	 * 
	 * @param obj
	 */
	public void touch(T obj) {
		recentReferences.put(obj.getGuid(), obj);
		obj.setCacheAge(0);			
		poke(obj);
	}
	
	/**
	 * Adds an explicit "strong" reference to a LiveObject;
	 * the object will not be evicted until dropStrongReference
	 * is invoked.
	 * 
	 * @param obj
	 */
	public void addStrongReference(T obj) {
		Guid guid = obj.getGuid();
		strongReferences.put(guid, obj);

	}
	
	/**
	 * Records a LiveObject as being a candidate for eviction,
	 * assuming it has not been recently accessed.
	 * 
	 * @param obj
	 */
	public void dropStrongReference(T obj) {
		Guid guid = obj.getGuid();
		strongReferences.remove(guid);
	}
	
	/**
	 * Returns a modifiable collection view of the recent
	 * references; this is used by the aging thread.
	 */
	public Collection<T> getRecentCache() {
		return recentReferences.values();
	}
	
	/**
	 * Returns an unmodifiable copy of the current set
	 * of strong references.
	 */
	public Set<T> getStrongReferenceCopy() {
		return Collections.unmodifiableSet(new HashSet<T>(strongReferences.values()));
	}		
	
	/**
	 * Returns an unmodifiable copy of the entire weak
	 * cache; this is useful for debugging purposes, e.g.
	 * the admin page.
	 */
	public Set<T> getWeakCacheCopy() {
		Set<T> ret = new HashSet<T>();
		for (GuidReference<T> ref : weakReferences.values()) {
			T obj = ref.get();
			if (obj != null)
				ret.add(obj);
		}
		return Collections.unmodifiableSet(ret);
	}
	
	/**
	 * Insert a modified LiveObject into the cache.    
	 */
	public void update(T obj) {
		T old = peek(obj.getGuid());
		// This guid must already exist in the cache, and
		// the object being inserted must not be the same
		// one as already exists.
		assert(old != null && old != obj);
		if (strongReferences.containsKey(obj.getGuid())) {
			strongReferences.put(obj.getGuid(), obj);
		}
		poke(obj);
		touch(obj);
	}		
	
	/**
	 * Invoke periodically to clean up the weak reference cache.
	 */
	public void clean() {
		while (true) {
			GuidReference<? extends T> reference = (GuidReference<? extends T>)queue.poll();
			if (reference == null)
				break;
			weakReferences.remove(reference.guid);
		}
	}
}
