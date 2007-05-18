package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class DMCache {
	protected static final Logger logger = GlobalSetup.getLogger(DMCache.class);
	
	private DMSessionMap sessionMap = new DMSessionMapJTA();
	private EntityManagerFactory emf = null;
	private Map<Class, DMClassHolder> classes = new HashMap<Class, DMClassHolder>();
	private ClassPool classPool = new ClassPool();
	private static DMCache instance = new DMCache();

	private DMCache() {
		classPool = new ClassPool();
	
		// FIXME. We actually want the class path to be the class path of the class loader
		// where the DMO's live. Something to fix when we add jar-file scanning to 
		// find DMOs.
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
	}
	
	public static DMCache getInstance() {
		return instance;
	}

	/**
	 * Public for use in tests which aren't using JTA transactions
	 * 
	 * @param sessionMap the session map
	 */
	public void setSessionMap(DMSessionMap sessionMap) {
		this.sessionMap = sessionMap;
	}
		
	/**
	 * Public for use in tests which aren't using container-managed entity-managers
	 * 
	 * @param sessionMap the session map
	 */
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}
	
	/**
	 * Add a DMO class to those managed by the cache. Eventually, we probably want
	 * to search for classes marked with @DMO rather than requiring manual 
	 * registration. 
	 * 
	 * @param <T>
	 * @param clazz
	 */
	public <T> void addDMClass(Class<T> clazz) {
		classes.put(clazz, new DMClassHolder<T>(this, clazz));
	}
	
	/**
	 * Creates an entity manager for use in injection into DMOs 
	 */
	public EntityManager createInjectableEntityManager() {
		return emf.createEntityManager();
	}
	
	public <T extends DMObject<?>> DMClassHolder<T> getDMClass(Class<T> clazz) {
		@SuppressWarnings("unchecked")
		DMClassHolder<T> dmClass = classes.get(clazz);
		
		return dmClass;
	}
	
	public void initializeReadOnlySession(DMViewpoint viewpoint) {
		DMSession session = new ReadOnlySession(this, viewpoint);
		sessionMap.initCurrent(session);
	}
	
	public void initializeReadWriteSession(DMViewpoint viewpoint) {
		DMSession session = new ReadWriteSession(this, viewpoint);
		sessionMap.initCurrent(session);
	}
	
	protected DMSession getCurrentSession() {
		DMSession session = sessionMap.getCurrent();
		if (session == null)
			throw new IllegalStateException("DM session wasn't initialized");

		return session;
	}
	
	public ClassPool getClassPool() {
		return classPool;
	}
	
	private static class PropertyKey<K,T> {
		private Class<T> resourceClass;
		private K key;
		private String propertyName;

		public PropertyKey(Class<T> resourceClass, K key, String propertyName) {
			this.resourceClass = resourceClass;
			this.key = key;
			this.propertyName = propertyName;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PropertyKey))
				return false;
			
			PropertyKey<?,?> other = (PropertyKey<?,?>)o;
			
			return resourceClass == other.resourceClass &&
				key.equals(other.key) &&
				propertyName.equals(other.propertyName);
		}
		
		@Override
		public int hashCode() {
			return 11 * resourceClass.hashCode() + 17 * key.hashCode() + 23 * propertyName.hashCode();
		}
	}
	
	private Map<PropertyKey, Object> cachedProperties = new ConcurrentHashMap<PropertyKey, Object>();
	private static Object nil = new Boolean(false);
	
	public <K, T extends DMObject<K>> Object fetchFromCache(Class<T> clazz, K key, String propertyName) throws NotCachedException {
		Object value = cachedProperties.get(new PropertyKey<K,T>(clazz, key, propertyName));
		if (value == null)
			throw new NotCachedException();
		else if (value == nil)
			return null;
		else
			return value;
	}

	public <K, T extends DMObject<K>> void storeInCache(Class<T> clazz, K key, String propertyName, Object value) {
		if (value == null)
			value = nil;
		
		cachedProperties.put(new PropertyKey<K,T>(clazz, key, propertyName), value);
	}
}
