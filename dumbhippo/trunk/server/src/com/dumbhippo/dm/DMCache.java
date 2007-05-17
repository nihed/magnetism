package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class DMCache {
	protected static final Logger logger = GlobalSetup.getLogger(DMCache.class);
	
	private DMSessionMap sessionMap = new DMSessionMapJTA();
	private EntityManagerFactory emf = null;
	private Map<Class, DMClass> classes = new HashMap<Class, DMClass>();

	private static DMCache instance = new DMCache();

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
		classes.put(clazz, new DMClass<T>(this, clazz));
	}
	
	/**
	 * Creates an entity manager for use in injection into DMOs 
	 */
	public EntityManager createInjectableEntityManager() {
		return emf.createEntityManager();
	}
	
	public <T extends DMObject<?>> DMClass<T> getDMClass(Class<T> clazz) {
		@SuppressWarnings("unchecked")
		DMClass<T> dmClass = classes.get(clazz);
		
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
}
