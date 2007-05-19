package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import javassist.ClassClassPath;
import javassist.ClassPool;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.cache.Timestamper;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class DataModel {
	protected static final Logger logger = GlobalSetup.getLogger(DataModel.class);
	
	private DMSessionMap sessionMap = new DMSessionMapJTA();
	private EntityManagerFactory emf = null;
	private Map<Class, DMClassHolder> classes = new HashMap<Class, DMClassHolder>();
	private ClassPool classPool = new ClassPool();
	private DMStore store = new DMStore();

	private static DataModel instance = new DataModel();

	private DataModel() {
		classPool = new ClassPool();
	
		// FIXME. We actually want the class path to be the class path of the class loader
		// where the DMO's live. Something to fix when we add jar-file scanning to 
		// find DMOs.
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
	}
	
	public static DataModel getInstance() {
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
	public <T extends DMObject> void addDMClass(Class<T> clazz) {
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
		
		if (dmClass == null)
			throw new IllegalArgumentException("Class " + clazz.getName() + " is not bound as a DMO");
		
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
	
	public DMStore getStore() {
		return store;
	}

	public long getTimestamp() {
		// FIXME: This doesn't fully work in a clustered configuration; we should use
		// timestamps/serials from the invalidation protocol instead.
		
		return Timestamper.next();
	}
}
