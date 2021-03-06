package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javassist.ClassClassPath;
import javassist.ClassPool;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.cache.Timestamper;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.DMStore;
import com.dumbhippo.dm.store.StoreClient;

/**
 * A DataModel is a central object holding information about entire data model; this 
 * includes schema information (which resource classes there are, which
 * properties do these classes have), cached resource property data, and information
 * about which clients are connected to the server and which resource properties
 * each client has registered for notification.  
 * 
 * @author otaylor
 */
public class DataModel {
	protected static final Logger logger = GlobalSetup.getLogger(DataModel.class);
	
	private String baseUrl;
	private DMSessionMap sessionMap = new DMSessionMapJTA();
	private EntityManagerFactory emf = null;
	private DMInjectionLookup injectionLookup;
	private ChangeNotifier notifier;
	private Map<Class<?>, DMClassHolder<?,?>> classes = new HashMap<Class<?>, DMClassHolder<?,?>>();
	private Map<String, DMClassHolder<?,?>> classesByBase = new HashMap<String, DMClassHolder<?,?>>();
	private ClassPool classPool;
	private DMStore store = new DMStore();
	private boolean completed = false;
	private ExecutorService notificationExecutor = ThreadUtils.newSingleThreadExecutor("DataModel-notification");
	private Class<? extends DMViewpoint> viewpointClass;
	private DMViewpoint systemViewpoint;

	/**
	 * Create a new DataModel object 
	 * 
	 * @param baseUrl base URL for all resources managed by this model
	 * @param sessionMap Session map object that handles associating sessions with
	 *   transactions
	 * @param emf Entity manager factory to create entity managers for injection into
	 *   sessions
	 * @param notifier object responsible for sending notifications to other cluster
	 *   nodes. May be null in the non-clustered case
	 * @param viewpointClass the class of the viewpoint objects that will be used with this model
	 *   (might be the base class of a hierarchy)
	 * @param systemViewpoint  the system viewpoint for this data model; this system viewpoint 
	 *   is a viewpoint that is allowed to see all data.
	 */
	public DataModel(String                       baseUrl,
			         DMSessionMap                 sessionMap,
			         EntityManagerFactory         emf,
			         DMInjectionLookup            injectionLookup,
			         ChangeNotifier               notifier,
			         Class<? extends DMViewpoint> viewpointClass,
			         DMViewpoint                  systemViewpoint) {
		
		if (baseUrl.endsWith("/"))
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		this.baseUrl = baseUrl;
		
		classPool = new ClassPool();
	
		// FIXME. We actually want the class path to be the class path of the class loader
		// where the DMO's live. Something to fix when we add jar-file scanning to 
		// find DMOs.
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
		
		this.sessionMap = sessionMap;
		this.emf = emf;
		this.injectionLookup = injectionLookup;
		this.notifier = notifier;
		this.viewpointClass = viewpointClass;
		this.systemViewpoint = systemViewpoint;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
	
	public DMViewpoint getSystemViewpoint() {
		return systemViewpoint;
	}
	
	/**
	 * Add a DMO class to those managed by the cache. Eventually, we probably want
	 * to search for classes marked with @DMO rather than requiring manual 
	 * registration. After adding all classes, call completeDMClasses(). 
	 * 
	 * @param <T>
	 * @param clazz
	 */
	public <K, T extends DMObject<K>> void addDMClass(Class<T> clazz) {
		if (completed)
			throw new IllegalStateException("completeDMClasses has already been callled");

		if (classes.get(clazz) != null) // Already added
			return;
		
		DMClassHolder<?,?> classHolder = DMClassHolder.createForClass(this, clazz);
		classes.put(clazz, classHolder);
		classesByBase.put(classHolder.getResourceBase(), classHolder);
	}
	
	/**
	 * Do any necessary post-processing after all DMO classes managed by the cache
	 * have been added.
	 */
	public void completeDMClasses() {
		if (completed)
			throw new IllegalStateException("completeDMClasses has already been callled");

		completed = true;
		
		for (DMClassHolder<?,?> classHolder : classes.values())
			classHolder.complete();
	}
	
	/**
	 * Creates an entity manager for use in injection into DMOs 
	 */
	public EntityManager createInjectableEntityManager() {
		return emf.createEntityManager();
	}
	
	public DMInjectionLookup getInjectionLookup() {
		return injectionLookup;
	}
	
	public DMClassHolder<?, ? extends DMObject<?>> getClassHolder(Class<?> tClass) {
		DMClassHolder<?,?> classHolder = classes.get(tClass);
		
		if (classHolder == null)
			throw new IllegalArgumentException("Class " + tClass.getName() + " is not bound as a DMO");
		
		return classHolder;
	}
	
	public DMClassHolder<?, ?> getClassHolder(String relativeBase) {
		return classesByBase.get(relativeBase);
	}

	@SuppressWarnings("unchecked")
	private <K, T extends DMObject<K>> DMClassHolder<K,T> castClassHolder(DMClassHolder<?,?> holder, Class<K> keyClass, Class<T> objectClass) {
		return (DMClassHolder<K,T>) holder;	
	}
	
	public <K, T extends DMObject<K>> DMClassHolder<K,T> getClassHolder(Class<K> keyClass, Class<T> objectClass) {
		DMClassHolder<K,T> classHolder =  castClassHolder(classes.get(objectClass), keyClass, objectClass);
		
		if (classHolder == null)
			throw new IllegalArgumentException("Class " + objectClass.getName() + " is not bound as a DMO");
		
		return classHolder;
	}

	private ReadOnlySession initializeReadOnlySession(DMClient client, DMViewpoint viewpoint) {
		ReadOnlySession session = new ReadOnlySession(this, client, viewpoint);
		sessionMap.initCurrent(session);
		
		return session;
	}
	
	private ReadWriteSession initializeReadWriteSession(DMClient client, DMViewpoint viewpoint) {
		ReadWriteSession session = new ReadWriteSession(this, client, viewpoint);
		sessionMap.initCurrent(session);
		
		return session;
	}

	public ReadOnlySession initializeReadOnlySession(DMClient client) {
		return initializeReadOnlySession(client, client.createViewpoint());
	}
	
	public ReadWriteSession initializeReadWriteSession(DMClient client) {
		return initializeReadWriteSession(client, client.createViewpoint());
	}
	
	public ReadOnlySession initializeReadOnlySession(DMViewpoint viewpoint) {
		return initializeReadOnlySession(null, viewpoint);
	}
	
	public ReadWriteSession initializeReadWriteSession(DMViewpoint viewpoint) {
		return initializeReadWriteSession(null, viewpoint);
	}

	public ReadOnlySession currentSessionRO() {
		DMSession session = sessionMap.getCurrent();
		if (!(session instanceof ReadOnlySession))
			throw new IllegalStateException("currentSessionRO() called when not inside a ReadOnlySession");
		
		return (ReadOnlySession)session;
	}
	
	public ReadWriteSession currentSessionRW() {
		DMSession session = sessionMap.getCurrent();
		if (!(session instanceof ReadWriteSession))
			throw new IllegalStateException("currentSessionRW() called when not inside a ReadWriteSession");
		
		return (ReadWriteSession)session;
	}
	
	/**
	 * Gets the current session, whether it is read-only or read-write. Usually you want
	 * to use currentSessionRO or currentSessionRW instead (which is why this isn't 
	 * exposed as a convenience method on DataService). Throws IllegalStateException 
	 * if there is no current session.
	 * 
	 * @return the current session
	 */
	public DMSession currentSession() {
		DMSession session = sessionMap.getCurrent();
		if (session == null)
			throw new IllegalStateException("currentSession() called when not inside a session");
		
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
	
	// This should return a ChangeNotiifcation<K, ? extends T>, like classHolder.makeChangeNotification(), but
	// that confuses javac (Java 5) in ways I can't figure out. Practically speaking, it 
	// doesn't end up mattering
	@SuppressWarnings("unchecked")
	protected <K, T extends DMObject<K>> ChangeNotification<K,T> makeChangeNotification(Class<T> clazz, K key, ClientMatcher matcher) {
		DMClassHolder<K,T> classHolder = (DMClassHolder<K,T>)getClassHolder(clazz);
		return (ChangeNotification<K, T>) classHolder.makeChangeNotification(key, matcher);
	}

	/**
	 * Send out local notifications resulting from a ReadWriteSession. Normally this 
	 * happens automatically and you must not call this function automatically, but
	 * the automatic sending can be disabled with {@link ChangeNotificationSet#setAutoNotify(boolean)}
	 * if you want explicit control over ordering.	
	 * 
	 * @param notifications the change notification set created in a ReadWriteSession
	 */
	public void sendNotifications(ChangeNotificationSet notifications) {
		logger.debug("Sending notifications for {}", notifications);
		ClientNotificationSet clientNotifications = notifications.resolveNotifications(this);
		
		logger.debug("Resolved notifications are {}", clientNotifications);
		for (ClientNotification clientNotification : clientNotifications.getNotifications()) {
			sendNotification(clientNotification);
		}
	}
	
	public void notifyRemoteChange(final ChangeNotificationSet notifications) {
		notifications.doInvalidations(this);
		sendNotifications(notifications);
	}
	
	protected void commitChanges(final ChangeNotificationSet notifications) {
		try {
			if (notifications.isEmpty())
				return;
			
			notifications.setTimestamp(getTimestamp());
			if (notifier != null)
				notifier.broadcastNotify(notifications);
			
			notifications.doInvalidations(this);
			
			if (notifications.getAutoNotify()) {
				notificationExecutor.execute(new Runnable() {
					public void run() {
						sendNotifications(notifications);
					}
				});
			}
		} catch (Exception e) {
			// Since we are running afterCompletion, exceptions get swallowed
			// so trap and log here
			logger.error("Failed to commit changes", e);
		}
	}
	
	/**
	 * Testing hook, not fully thread safe.
	 */
	public void waitForAllNotifications() {
		ExecutorService oldExecutor = notificationExecutor;
		notificationExecutor = ThreadUtils.newSingleThreadExecutor("DataModel-notification");
		
		oldExecutor.shutdown();
		try {
			oldExecutor.awaitTermination(300, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Timed out waiting for pending notifications to clear");
		}
	}

	public void sendNotification(final ClientNotification clientNotification) {
		logger.debug("Sending notification to {}", clientNotification.getClient());
		
		sessionMap.runInTransaction(new Runnable() {
			public void run() {
				StoreClient client = clientNotification.getClient();
				
				long serial = client.allocateSerial(); 
				
				boolean success = false;
				try {
					ReadOnlySession session = initializeReadOnlySession(client);
					
					FetchVisitor visitor = client.beginNotification();
					clientNotification.visitNotification(session, visitor);
					client.endNotification(visitor, serial);
					success = true;
				} finally {
					if (!success)
						client.nullNotification(serial);
				}
			}
		});
	}
	
	public Class<? extends DMViewpoint> getViewpointClass() {
		return viewpointClass;
	}
}
