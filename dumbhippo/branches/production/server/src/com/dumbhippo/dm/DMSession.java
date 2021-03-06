package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
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
		@SuppressWarnings("unchecked")
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
	public <T extends DMObject<?>> void internalInit(T t) {
		try {
			doInit(t);
		} catch (NotFoundException e) {
			throw new LazyInitializationException("NotFoundException when lazily initializing DMO; improper use of findUnchecked() or deleted object?", e);
		}
	}
	
	private <K, T extends DMObject<K>> StoreKey<K,T> makeStoreKey(Class<T> clazz, K key) {
		@SuppressWarnings("unchecked")
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)model.getClassHolder(clazz); 
		return new StoreKey<K,T>(classHolder, key);
	}
	
	private <T extends DMObject<?>>void doInit(T t) throws NotFoundException {
		@SuppressWarnings("unchecked")
		DMClassHolder<?,T> classHolder = t.getClassHolder(); 

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
	public <K, T extends DMObject<K>> Object getRawProperty(Class<T> clazz, K key, String propertyName) throws NotFoundException {
		StoreKey<K,T> storeKey = makeStoreKey(clazz, key);
		
		int propertyIndex = storeKey.getClassHolder().getPropertyIndex(propertyName);
		if (propertyIndex < 0)
			throw new RuntimeException("Class " + clazz.getName() + " has no property " + propertyName);

		// We look first in the cache, and only if that fails do we go ahead and create an 
		// object. The object we create is created from the special "unfiltered session",
		// which does no filtering and no caching.
		try {
			return model.getStore().fetch(storeKey, propertyIndex);
		} catch (NotCachedException e) {
			DMPropertyHolder propertyHolder = storeKey.getClassHolder().getProperty(propertyIndex);
			T object = model.getUnfilteredSession().findUnchecked(storeKey);

			// There is some inefficiency here ... the property value is dehydrated twice;
			// once when the property is cached in the cache as a side effect of fetching
			// it, and again here. But dehydration is pretty fast, and doing it this
			// way is robust against the case where the property is *not* cached.
			// (because it's viewer specific, because the property is immediately
			// evicted from the cache again, etc.)
			try {
				return propertyHolder.dehydrate(propertyHolder.getRawPropertyValue(object));
			} catch (LazyInitializationException e2) {
				throw e2.getCause();
			}
		}
	}
	
	public Object getInjectableEntityManager() {
		if (injectableEntityManager == null)
			injectableEntityManager = model.createInjectableEntityManager();
		
		return injectableEntityManager;
	}
	

	public abstract void afterCompletion(int status);
}
