package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.NotFoundException;

public class DMSession {
	static private Logger logger = GlobalSetup.getLogger(DMSession.class);

	private Map<Object, DMObject> sessionDMOs = new HashMap<Object, DMObject>();
	private DMCache cache;
	private DMViewpoint viewpoint;
	private EntityManager injectableEntityManager;
	
	protected DMSession(DMCache cache, DMViewpoint viewpoint) {
		this.cache = cache;
		this.viewpoint = viewpoint;
	}
	
	protected DMCache getCache() {
		return cache;
	}

	public DMViewpoint getViewpoint() {
		return viewpoint;
	}
	
	public <K, T extends DMObject<K>> T find(Class<T> clazz, K key) throws NotFoundException {
		@SuppressWarnings("unchecked")
		T result = (T)sessionDMOs.get(key);
		
		if (result == null) {
			logger.debug("Didn't find object for key {}, creating a new one", key);
			
			DMClass<T> dmClass = cache.getDMClass(clazz); 
			
			result = dmClass.createInstance(key);
			sessionDMOs.put(key, result);
			
			// FIXME: Just a temporary hack
			dmClass.processInjections(this, result);
			result.init();
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

	public Object getInjectableEntityManager() {
		if (injectableEntityManager == null)
			injectableEntityManager = cache.createInjectableEntityManager();
		
		return injectableEntityManager;
	}
}
