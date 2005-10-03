/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.dumbhippo.identity20.Guid;

/**
 * 
 * Storage connection. Based on Yarrr.java by Colin. Contains per-thread
 * SessionWrapper transaction/session object.
 * 
 * @author hp
 * @author walters
 * 
 */
public final class Storage {

	/**
	 * Wraps the Hibernate session object, adding some convenience functions.
	 * 
	 * @author hp
	 * 
	 */
	public class SessionWrapper {
		private Session session;

		private Transaction transaction;

		private SessionWrapper() {

		}

		/**
		 * Convenience method to load a GuidPersistable object using its Guid.
		 * Also calls setNewlyCreated(false) on the loaded object.
		 * 
		 * @param klass
		 *            the class to load
		 * @param guid
		 *            the ID of the class
		 * @return the object that was loaded
		 */
		public GuidPersistable loadFromGuid(Class klass, Guid guid)
				throws HibernateException {
			GuidPersistable object = (GuidPersistable) getSession().load(klass,
					guid.toString());

			return object;
		}
		
		public DBUnique loadFromId(Class klass, long id) 
			throws HibernateException {
			DBUnique object = (DBUnique) getSession().load(klass, id);
			
			return object;
		}
		
		
		/**
		 * Returns whether or not a GUID is currently known to exist
		 * in the database.
		 * 
		 * @param guid a potentially used guid
		 * @return true iff guid already exists
		 */
		public boolean guidExists(Guid guid) {
			return loadFromGuid(GuidPersistable.class, guid) == null;
		}
		
		public Session getSession() throws HibernateException {
			assert sessionFactory != null;
			if (session == null)
				session = sessionFactory.openSession();
			assert session != null;
			return session;
		}

		public boolean isTransactionActive() {
			return transaction != null;
		}

		/**
		 * Commits the current transaction and begins a new transaction in a new
		 * session. This is useful to really test database changes, since if you
		 * are using the same session you're just testing in-memory objects.
		 * 
		 * @throws HibernateException
		 */
		public void commitCloseBeginTransaction() throws HibernateException {
			commitTransaction();
			closeSession();
			beginTransaction();
		}

		public void closeSession() throws HibernateException {
			if (session != null && session.isOpen()) {
				// If there is an uncommited/rollbacked transaction open, try to
				// roll it back
				try {
					if (transaction != null && !transaction.wasCommitted()
							&& !transaction.wasRolledBack()) {
						transaction.rollback();
					}
				} catch (Exception e) {
					// Whatever, we tried at least
				}

				session.close();
				session = null;
				transaction = null;
			}
		}

		public void beginTransaction() throws HibernateException {
			if (transaction == null)
				transaction = getSession().beginTransaction();
		}

		public void commitTransaction() throws HibernateException {
			try {
				if (transaction != null && !transaction.wasCommitted()
						&& !transaction.wasRolledBack()) {
					transaction.commit();
				}
				transaction = null;
			} catch (HibernateException ex) {
				rollbackTransaction();
				throw ex;
			}
		}

		public void rollbackTransaction() throws HibernateException {
			try {
				if (transaction != null && !transaction.wasCommitted()
						&& !transaction.wasRolledBack()) {
					transaction.rollback();
				}
			} finally {
				closeSession();
			}
		}
	}

	private static class DefaultProperties {
		boolean embedded = false;

		String storagePath = null;

		DefaultProperties(Log logger) {
			logger.debug("Loading default properties for Storage");
			Properties props = new Properties();
			try {
				InputStream stream = Storage.class
						.getResourceAsStream("storage.properties");
				if (stream == null) {
					logger.warn("No storage.properties found");
				} else {
					logger.debug("Found a storage.properties");
					props.load(stream);
				}
			} catch (IOException e) {
				logger.warn("Couldn't open storage.properties", e);
			}

			this.embedded = Boolean.parseBoolean(props.getProperty("embedded",
					"false"));
			this.storagePath = props.getProperty("storagePath",
					"/tmp/hippo-storage");
		}
	}

	private static DefaultProperties defaults = null;

	private static Log logger = null;

	private static Storage globalInstance = null;

	private String storagePath = null;

	private boolean embedded = true;

	private SessionFactory sessionFactory = null;

	private ThreadLocal<SessionWrapper> perThreadSession = null;

	private Configuration configuration;

	/**
	 * Call this to create the singleton Storage globalInstance.
	 * 
	 * It sucks to have to call init(), but the storagePath argument makes it
	 * necessary since we don't want a function that can be called twice and
	 * takes an argument, since the second time the argument would be ignored.
	 * 
	 * There's no point making this thread safe because trying it from multiple
	 * threads doesn't make any sense (you can only call it once!)
	 * 
	 * @param storagePath
	 *            location for database.
	 */
	public static void initGlobalInstance(String storagePath) {
		if (globalInstance == null)
			globalInstance = new Storage(storagePath);
	}
	
	/**
	 * Shut down the well-known global instance; this method should generally
	 * only be used by the test suite
	 */
	public static void destroyGlobalInstance() {
		assert (globalInstance != null);
		globalInstance.shutdown();
		globalInstance = null;
	}

	public static Storage getGlobalInstance() {
		if (globalInstance == null)
			throw new Error("Have to call init() prior to getGlobalInstance()");
		return globalInstance;
	}

	public Storage() {
		this(null);
	}

	public Storage(String storagePath) {
		initializeLoggingAndDefaults();

		perThreadSession = new ThreadLocal<SessionWrapper>();

		if (storagePath == null)
			this.storagePath = defaults.storagePath;
		else
			this.storagePath = storagePath;

		embedded = defaults.embedded;

		if (embedded)
			logger.warn("Using embedded mode");

		openDB();
	}

	public SessionWrapper getPerThreadSession() throws HibernateException {
		SessionWrapper h = perThreadSession.get();

		if (h == null) {
			h = new SessionWrapper();
			perThreadSession.set(h);
		}

		return h;
	}

	public static SessionWrapper getGlobalPerThreadSession()
			throws HibernateException {
		Storage storage = getGlobalInstance();
		return storage.getPerThreadSession();
	}

	public SessionWrapper createNewSession() throws HibernateException {
		return new SessionWrapper();
	}

	private static void initializeLoggingAndDefaults() {
		if (logger == null) {
			assert defaults == null;
			logger = LogFactory.getLog(Storage.class);
			defaults = new DefaultProperties(logger);
		}
	}

	private enum DBOption {
		NONE, CREATE, SHUTDOWN
	}

	private String getJDBCUrl(DBOption... options) {
		StringBuilder jdbcURL;
		jdbcURL = new StringBuilder("jdbc:derby:");
		assert storagePath != null;
		jdbcURL.append(storagePath);
		for (DBOption opt : options) {
			if (opt == DBOption.CREATE) {
				logger.debug("CREATE option in jdbc URL");
				jdbcURL.append(";create=true");
			}
			if (opt == DBOption.SHUTDOWN) {
				logger.debug("SHUTDOWN option in jdbc URL");
				jdbcURL.append(";shutdown=true");
			}
		}
		return jdbcURL.toString();
	}

	private void openDB() {
		boolean dbExists = (new File(storagePath)).exists();

		logger.debug("dbExists = " + dbExists);
		
		String dbURL;
		if (!dbExists)
			dbURL = getJDBCUrl(DBOption.CREATE);
		else
			dbURL = getJDBCUrl(DBOption.NONE);

		logger.info("Connecting to JDBC url: " + dbURL);

		configuration = buildHibernateConfiguration(dbURL);

		logger.info("Creating SessionWrapper session factory");

		try {
			sessionFactory = configuration.buildSessionFactory();
		} catch (HibernateException e) {
			// SessionWrapper will complain here for various reasons if there
			// are
			// problems with the classes
			// we'll be persisting (e.g. errors in the .hbm.xml)
			// Unfortunately this code gets called ATM from a static
			// initialiser, and it can be difficult
			// to see where the exception gets thrown. Printing it out here
			// should make it easier to spot problems.
			System.err.println(e.getMessage());
			throw e;
		}
	}

	public void shutdown() throws HibernateException {

		if (this == globalInstance) {
			logger.debug("shutting down global Storage instance");
			globalInstance = null;
		}

		logger.info("Shutting down storage");

		sessionFactory.close();
		sessionFactory = null;
		perThreadSession = null;

		String url = getJDBCUrl(DBOption.SHUTDOWN);
		try {
			DriverManager.getConnection(url);
		} catch (Exception e) {
			logger.info("JDBC database shutdown");
		}
	}

	private static Configuration buildHibernateConfiguration(
			String connectionUrl) throws HibernateException {

		logger.debug("building hibernate configuration");

		Configuration cfg = new Configuration();

		cfg.setProperty("hibernate.connection.driver_class",
				"org.apache.derby.jdbc.EmbeddedDriver");
		cfg.setProperty("hibernate.dialect",
				"org.hibernate.dialect.DerbyDialect");
		cfg.setProperty("hibernate.connection.url", connectionUrl);

		// Resource class .hbm.xml includes all Resource subclasses
		logger.debug("adding Resource class def to hibernate configuration");
		cfg.addResource("com/dumbhippo/persistence/Hibernate.hbm.xml");		
		
		cfg.setProperty(Environment.HBM2DDL_AUTO, "update");

		return cfg;
	}
}
