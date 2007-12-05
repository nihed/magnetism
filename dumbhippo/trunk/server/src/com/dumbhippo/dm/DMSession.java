package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.server.NotFoundException;

public abstract class DMSession {
	protected DataModel model;
	private EntityManager injectableEntityManager;
	
	protected DMSession(DataModel model) {
		this.model = model;
	}
	
	public DataModel getModel() {
		return model;
	}

	public abstract DMClient getClient();
	public abstract DMViewpoint getViewpoint();
	
	public abstract <K, T extends DMObject<K>> T find(StoreKey<K,T> storeKey) throws NotFoundException;

	public <K, T extends DMObject<K>> T find(Class<T> clazz, K key) throws NotFoundException {
		return find(makeStoreKey(clazz, key));
	}
	
	public abstract <K, T extends DMObject<K>> T findUnchecked(StoreKey<K,T> storeKey);
	
	/**
	 * Like find(), but doesn't check to make sure that the object exists, and doesn't
	 * perform security checks to make sure that the viewer is supposed to be able
	 * to see the object. The only really appropriate place to use this method is inside
	 * methods of a DMO where the result will be filtered by the system. Do *not* use
	 * this method inside a DMO method marked as viewer-dependent.
	 * 
	 * (Or should we make viewer-dependent methods still filter to remove that trap?
	 * if we did that, we could make use a different sub-interface of DMSession for
	 * the session injected into a DMObject than the one returned by getCurrent().)
	 * 
	 * If key doesn't refer to an object that exists, later access to the object's
	 * properties will throw LazyInitializationException.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @return
	 */
	public <K, T extends DMObject<K>> T findUnchecked(Class<T> clazz, K key) {
		return findUnchecked(makeStoreKey(clazz, key));
	}
	
	@SuppressWarnings("unchecked")
	private DMObject find(DMClassHolder classHolder, String keyString) throws NotFoundException {
		if (classHolder == null)
			throw new NotFoundException("Unknown type of resource");
		
		try {
			StoreKey storeKey = classHolder.makeStoreKey(keyString);
			return find(storeKey);
		} catch (BadIdException e) {
			throw new NotFoundException("Bad ID in resourceId: " + e.getMessage(), e);
		}
	}
	
	public DMObject<?> find(String resourceId) throws NotFoundException {
		if (!resourceId.startsWith(model.getBaseUrl()))
			throw new NotFoundException("resourceId doesn't identify a resource on this server: " + resourceId);
		int relativeBaseStart = model.getBaseUrl().length();
		int lastSlash = resourceId.lastIndexOf('/');
		if (lastSlash < 0 || lastSlash < relativeBaseStart)
			throw new NotFoundException("Badly formed resourceId: " + resourceId);
		String relativeBase = resourceId.substring(relativeBaseStart, lastSlash);
		DMClassHolder<?,?> classHolder = model.getClassHolder(relativeBase);
		if (classHolder == null)
			throw new NotFoundException("Unknown type of resource in resourceId: " + resourceId);
		
		return find(classHolder, resourceId.substring(lastSlash + 1));
	}
	
	public <K,T extends DMObject<K>> void visitFetch(T object, Fetch<K,? super T> fetch, FetchVisitor visitor) {
		DMClassHolder<K,T> classHolder = object.getClassHolder();
		fetch.visit(this, classHolder, object, visitor);
	}

	/**
	 * For use in generated code; this isn't part of the public interface 
	 * 
	 * @param <T>
	 * @param clazz
	 * @param t
	 */
	public <K, T extends DMObject<K>> void internalInit(T t) {
		try {
			doInit(t);
		} catch (NotFoundException e) {
			throw new LazyInitializationException("NotFoundException when lazily initializing DMO; improper use of findUnchecked() or deleted object?", e);
		}
	}
	
	// This "casts" two classes K and T to be related, note we need a K and a T in order to construct
	// StoreKey without a warning about a bare type
	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> StoreKey<K,T> newStoreKeyHack(DMClassHolder<?,? extends DMObject<?>> classHolder,
			K key) {
		return new StoreKey<K,T>((DMClassHolder<K,T>) classHolder, key);
	}
	
	protected <K, T extends DMObject<K>> StoreKey<K,T> makeStoreKey(Class<T> clazz, K key) {
		DMClassHolder<?,? extends DMObject<?>> classHolder = model.getClassHolder(clazz); 
		return newStoreKeyHack(classHolder, key);
	}
	
	private <K, T extends DMObject<K>> void doInit(T t) throws NotFoundException {
		DMClassHolder<K,T> classHolder = t.getClassHolder();

		classHolder.processInjections(this, t);
		t.init();
	}

	protected <K, T extends DMObject<K>> void ensureExists(StoreKey<K,T> key, T t) throws NotFoundException {
		if (t.isInitialized())
			return;
		
		if (model.getStore().checkCached(key))
			return;
		
		doInit(t);
	}
	
	/**
	 * Internal API: looks for cached data for the specified resource property. If found, filters it
	 *   for this session's viewpoint, and returns the result.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @param propertyIndex
	 * @return
	 * @throws NotCachedException
	 */
	public abstract <K, T extends DMObject<K>> Object fetchAndFilter(StoreKey<K,T> key, int propertyIndex) throws NotCachedException;

	/**
	 * Internal API: Stores the specified (unfiltered) value in the cache, then filters it
	 *   for this session's viewpoint, and returns the result.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @param propertyIndex
	 * @param value
	 * @return
	 * @throws NotCachedException
	 */
	public abstract <K, T extends DMObject<K>> Object storeAndFilter(StoreKey<K,T> key, int propertyIndex, Object value);
	
	/**
	 * Internal API: Create a feed wrapper that handles filtering and caching for for the raw feed object. 
	 * 
	 * @param <K>
	 * @param <T>
	 * @param <TI>
	 * @param key
	 * @param propertyIndex
	 * @param rawFeed
	 * @return
	 */
	public abstract <K, T extends DMObject<K>> DMFeed<?> createFeedWrapper(StoreKey<K,T> key, int propertyIndex, DMFeed<T> rawFeed);

	/**
	 * Finds the "raw" value for a particular object property. Raw values differ from the normally
	 * fetched values in a couple of ways. First, they aren't filtered: they are returned as if
	 * viewed from the system viewpoint. Second, for resource-valued properties, what is returned
	 * is the key of the property value (or, for a multi-valued property, a list of keys) rather
	 * than the DMObject. More obviously, primitive types are boxed on return.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @param propertyName
	 * @return 
	 * @throws NotFoundException
	 */
	public abstract <K, T extends DMObject<K>> Object getRawProperty(Class<T> clazz, K key, String propertyName) throws NotFoundException;
	
	public Object getInjectableEntityManager() {
		if (injectableEntityManager == null)
			injectableEntityManager = model.createInjectableEntityManager();
		
		return injectableEntityManager;
	}
	

	public abstract void afterCompletion(int status);
}
