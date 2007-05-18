package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.NotFoundException;

public abstract class DMSession {
	static private Logger logger = GlobalSetup.getLogger(DMSession.class);

	private Map<Object, DMObject> sessionDMOs = new HashMap<Object, DMObject>();
	protected DataModel model;
	private DMViewpoint viewpoint;
	private EntityManager injectableEntityManager;
	
	protected DMSession(DataModel model, DMViewpoint viewpoint) {
		this.model = model;
		this.viewpoint = viewpoint;
	}
	
	protected DataModel getModel() {
		return model;
	}

	public DMViewpoint getViewpoint() {
		return viewpoint;
	}
	
	public <K, T extends DMObject<K>> T find(Class<T> clazz, K key) throws NotFoundException {
		@SuppressWarnings("unchecked")
		T result = (T)sessionDMOs.get(key);
		
		if (result == null) {
			logger.debug("Didn't find object for key {}, creating a new one", key);
			
			DMClassHolder<T> dmClass = model.getDMClass(clazz); 
			
			result = dmClass.createInstance(key, this);
			sessionDMOs.put(key, result);
		}
		
		return result;
	}
	
	public <K, T extends DMObject<K>> T findMustExist(Class<T> clazz, K key) {
		try {
			return find(clazz, key);
		} catch (NotFoundException e) {
			throw new RuntimeException("Entity unexpectedly missing, class=" + clazz.getName() + ", key=" + key);
		}
	}
	
	/**
	 * For use in generated code; this isn't part of the public interface 
	 * 
	 * @param <T>
	 * @param clazz
	 * @param t
	 */
	public <T extends DMObject<?>> void internalInit(Class<T> clazz, T t) {
		DMClassHolder<T> dmClass = model.getDMClass(clazz); 
		
		dmClass.processInjections(this, t);
		
		// FIXME: sort this out, or at least throw a specific unchecked exception
		//   If we init() immediately on objects not found in the cache we
		//   could cut down on having to expect lazy exceptions.
		try {
			t.init();
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Internal API: looks for cached data for the specified resource property. If found, filters it
	 *   for this session's viewpoint, and returns the result.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @param propertyName
	 * @return
	 * @throws NotCachedException
	 */
	public abstract <K, T extends DMObject<K>> Object fetchAndFilter(Class<T> clazz, K key, String propertyName) throws NotCachedException;

	/**
	 * Internal API: Stores the specified (unfiltered) value in the cache, then filters it
	 *   for this session's viewpoint, and returns the result.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param clazz
	 * @param key
	 * @param propertyName
	 * @return
	 * @throws NotCachedException
	 */
	public abstract <K, T extends DMObject<K>> Object storeAndFilter(Class<T> clazz, K key, String propertyName, Object value);

	public Object getInjectableEntityManager() {
		if (injectableEntityManager == null)
			injectableEntityManager = model.createInjectableEntityManager();
		
		return injectableEntityManager;
	}
}
