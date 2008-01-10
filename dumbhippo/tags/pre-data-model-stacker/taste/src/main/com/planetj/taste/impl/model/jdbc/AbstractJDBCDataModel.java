/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.impl.model.jdbc;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.impl.common.IteratorIterable;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.transforms.PreferenceTransform;

import org.jetbrains.annotations.NotNull;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Owen
 */
public abstract class AbstractJDBCDataModel implements DataModel {

	private static final Logger log = Logger.getLogger(AbstractJDBCDataModel.class.getName());

	public static final String DEFAULT_DATASOURCE_NAME = "jdbc/taste";
	public static final String DEFAULT_PREFERENCE_TABLE = "taste_preferences";
	public static final String DEFAULT_USER_ID_COLUMN = "user_id";
	public static final String DEFAULT_ITEM_ID_COLUMN = "item_id";
	public static final String DEFAULT_PREFERENCE_COLUMN = "preference";

	private final DataSource dataSource;
	private final List<PreferenceTransform> transforms;
	private final String getUserSQL;
	private final String getNumItemsSQL;
	private final String getNumUsersSQL;
	private final String setPreferenceSQL;
	private final String getUsersSQL;
	private final String getItemsSQL;
	private final String getItemSQL;
	private final String getPrefsForItemSQL;
	private final ReentrantLock refreshLock;

	protected AbstractJDBCDataModel(final DataSource dataSource,
	                                final String getUserSQL,
	                                final String getNumItemsSQL,
	                                final String getNumUsersSQL,
	                                final String setPreferenceSQL,
	                                final String getUsersSQL,
	                                final String getItemsSQL,
	                                final String getItemSQL,
	                                final String getPrefsForItemSQL) {

		log.fine("Creating AbstractJDBCModel...");
		checkNotNullAndLog("dataSource", dataSource);
		checkNotNullAndLog("getUserSQL", getUserSQL);
		checkNotNullAndLog("getNumItemsSQL", getNumItemsSQL);
		checkNotNullAndLog("getNumUsersSQL", getNumUsersSQL);
		checkNotNullAndLog("setPreferenceSQL", setPreferenceSQL);
		checkNotNullAndLog("getUsersSQL", getUsersSQL);
		checkNotNullAndLog("getItemsSQL", getItemsSQL);
		checkNotNullAndLog("getItemSQL", getItemSQL);
		checkNotNullAndLog("getPrefsForItemSQL", getPrefsForItemSQL);

		this.dataSource = dataSource;
		this.transforms = Collections.synchronizedList(new ArrayList<PreferenceTransform>());
		this.getUserSQL = getUserSQL;
		this.getNumItemsSQL = getNumItemsSQL;
		this.getNumUsersSQL = getNumUsersSQL;
		this.setPreferenceSQL = setPreferenceSQL;
		this.getUsersSQL = getUsersSQL;
		this.getItemsSQL = getItemsSQL;
		this.getItemSQL = getItemSQL;
		this.getPrefsForItemSQL = getPrefsForItemSQL;
		this.refreshLock = new ReentrantLock();
	}

	private static void checkNotNullAndLog(final String argName, final Object value) {
		if (value == null || value.toString().length() == 0) {
			throw new IllegalArgumentException(argName + " is null or empty");
		}
		if (log.isLoggable(Level.FINE)) {
			log.fine(argName + ": " + value);
		}
	}

	/**
	 * <p>Looks up a {@link DataSource} by name from JNDI.</p>
	 *
	 * @param dataSourceName JNDI name where a {@link DataSource} is bound (e.g. "jdbc/taste")
	 * @return {@link DataSource} under that JNDI name
	 * @throws TasteException if a JNDI error occurs
	 */
	protected static DataSource lookupDataSource(final String dataSourceName) throws TasteException {
		Context context = null;
		try {
			context = new InitialContext();
			return (DataSource) context.lookup(dataSourceName);
		} catch (NamingException ne) {
			throw new TasteException(ne);
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException ne) {
					log.log(Level.WARNING, "Error while closing Context; continuing...", ne);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public final Iterable<User> getUsers() throws TasteException {
		log.fine("Retrieving all users...");
		return new IteratorIterable<User>(new ResultSetUserIterator());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws java.util.NoSuchElementException
	 *          if there is no such user
	 */
	@NotNull
	public final User getUser(final Object id) throws TasteException {

		if (log.isLoggable(Level.FINE)) {
			log.fine("Retrieving user ID '" + id + "'...");
		}

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		final String idString = id.toString();
		final List<Preference> prefs = new ArrayList<Preference>();

		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(getUserSQL);
			stmt.setObject(1, id);

			if (log.isLoggable(Level.FINE)) {
				log.fine("Executing SQL query: " + getUserSQL);
			}
			rs = stmt.executeQuery();

			while (rs.next()) {
				addPreference(rs, prefs);
			}

			if (prefs.isEmpty()) {
				throw new NoSuchElementException();
			}

			final User user = buildUser(idString, prefs);

			transformUser(user);
			return user;

		} catch (SQLException sqle) {
			log.log(Level.WARNING, "Exception while retrieving user", sqle);
			throw new TasteException(sqle);
		} finally {
			safeClose(rs, stmt, conn);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public final Iterable<Item> getItems() throws TasteException {
		log.fine("Retrieving all items...");
		return new IteratorIterable<Item>(new ResultSetItemIterator());
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws java.util.NoSuchElementException
	 *          if there is no such user
	 */
	@NotNull
	public final Item getItem(final Object id) throws TasteException {

		if (log.isLoggable(Level.FINE)) {
			log.fine("Retrieving item ID '" + id + "'...");
		}

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(getItemSQL);
			stmt.setObject(1, id);

			if (log.isLoggable(Level.FINE)) {
				log.fine("Executing SQL query: " + getItemSQL);
			}
			rs = stmt.executeQuery();
			if (rs.next()) {
				return buildItem((String) id);
			} else {
				throw new NoSuchElementException();
			}
		} catch (SQLException sqle) {
			log.log(Level.WARNING, "Exception while retrieving item", sqle);
			throw new TasteException(sqle);
		} finally {
			safeClose(rs, stmt, conn);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public final Iterable<Preference> getPreferencesForItem(final Object itemID) throws TasteException {

		if (log.isLoggable(Level.FINE)) {
			log.fine("Retrieving preferences for item ID '" + itemID + "'...");
		}

		final Item item = getItem(itemID);

		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(getPrefsForItemSQL);
			stmt.setObject(1, itemID);

			if (log.isLoggable(Level.FINE)) {
				log.fine("Executing SQL query: " + getPrefsForItemSQL);
			}
			rs = stmt.executeQuery();
			final List<Preference> prefs = new ArrayList<Preference>();
			while (rs.next()) {
				final double preference = rs.getDouble(1);
				final String userID = rs.getString(2);
				final Preference pref = buildPreference(buildUser(userID, null), item, preference);
				prefs.add(pref);
			}
			return prefs;
		} catch (SQLException sqle) {
			log.log(Level.WARNING, "Exception while retrieving prefs for item", sqle);
			throw new TasteException(sqle);
		} finally {
			safeClose(rs, stmt, conn);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final int getNumItems() throws TasteException {
		return getNumThings("items", getNumItemsSQL);
	}

	/**
	 * {@inheritDoc}
	 */
	public final int getNumUsers() throws TasteException {
		return getNumThings("users", getNumUsersSQL);
	}

	private int getNumThings(final String name, final String sql) throws TasteException {
		log.fine("Retrieving number of " + name + " in model...");
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Executing SQL query: " + sql);
			}
			rs = stmt.executeQuery(sql);
			rs.next();
			return rs.getInt(1);
		} catch (SQLException sqle) {
			log.log(Level.WARNING, "Exception while retrieving number of " + name, sqle);
			throw new TasteException(sqle);
		} finally {
			safeClose(rs, stmt, conn);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void addTransform(final PreferenceTransform preferenceTransform) {
		if (preferenceTransform == null) {
			throw new IllegalArgumentException();
		}
		log.config("Adding preference transform '" + preferenceTransform + "'...");
		synchronized (transforms) {
			transforms.add(preferenceTransform);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setPreference(final Object userID, final Object itemID, final double value)
		throws TasteException {

		if (userID == null || itemID == null || Double.isNaN(value)) {
			throw new IllegalArgumentException();
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Setting preference for user '" + userID + "', item '" + itemID + "', value " + value);
		}

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = dataSource.getConnection();

			stmt = conn.prepareStatement(setPreferenceSQL);
			stmt.setObject(1, userID);
			stmt.setObject(2, itemID);
			stmt.setDouble(3, value);
			stmt.setDouble(4, value);

			if (log.isLoggable(Level.FINE)) {
				log.fine("Executing SQL update: " + setPreferenceSQL);
			}
			stmt.executeUpdate();

		} catch (SQLException sqle) {
			log.log(Level.WARNING, "Exception while setting preference", sqle);
			throw new TasteException(sqle);
		} finally {
			safeClose(null, stmt, conn);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void refresh() {
		if (refreshLock.isLocked()) {
			return;
		}
		refreshLock.lock();
		try {
			synchronized (transforms) {
				for (final PreferenceTransform transform : transforms) {
					transform.refresh();
				}
			}
		} finally {
			refreshLock.unlock();
		}
	}


	private void addPreference(final ResultSet rs, final List<Preference> prefs)
		throws SQLException {
		final Item item = buildItem(rs.getString(1));
		final double preferenceValue = rs.getDouble(2);
		prefs.add(buildPreference(null, item, preferenceValue));
	}

	private void transformUser(final User user) {
		synchronized (transforms) {
			for (final PreferenceTransform transform : transforms) {
				transform.transformPreferences(user);
			}
		}
	}

	/**
	 * Subclasses may override to return a different {@link User} implementation.
	 *
	 * @param id user ID
	 * @param prefs user preferences
	 * @return {@link GenericUser} by default
	 */
	protected User buildUser(final String id, final List<Preference> prefs) {
		return new GenericUser<String>(id, prefs);
	}

	/**
	 * Subclasses may override to return a different {@link Item} implementation.
	 *
	 * @param id item ID
	 * @return {@link GenericItem} by default
	 */
	protected Item buildItem(final String id) {
		return new GenericItem<String>(id);
	}

	/**
	 * Subclasses may override to return a different {@link Preference} implementation.
	 *
	 * @param user {@link User}
	 * @param item {@link Item}
	 * @return {@link GenericPreference} by default
	 */
	protected Preference buildPreference(final User user, final Item item, final double value) {
		return new GenericPreference(user, item, value);
	}

	private static void safeClose(final ResultSet resultSet,
	                              final Statement statement,
	                              final Connection connection) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException sqle) {
				log.log(Level.WARNING, "Unexpected exception while closing ResultSet", sqle);
			}
		}
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException sqle) {
				log.log(Level.WARNING, "Unexpected exception while closing Statement", sqle);
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException sqle) {
				log.log(Level.WARNING, "Unexpected exception while closing Connection", sqle);
			}
		}
	}

	/**
	 * <p>An {@link java.util.Iterator} which returns {@link com.planetj.taste.model.User}s from a {@link java.sql.ResultSet}. This is a useful
	 * way to iterate over all user data since it does not require all data to be read into memory
	 * at once. It does however require that the DB connection be held open. Note that this class will
	 * only release database resources after {@link #hasNext()} has been called and has returned false;
	 * callers should make sure to "drain" the entire set of data to avoid tying up database resources.</p>
	 *
	 * @author Sean Owen
	 */
	private final class ResultSetUserIterator implements Iterator<User> {

		private final Connection connection;
		private final Statement statement;
		private final ResultSet resultSet;
		private boolean closed;

		ResultSetUserIterator() throws TasteException {
			try {
				connection = dataSource.getConnection();
				statement = connection.createStatement();
				if (log.isLoggable(Level.FINE)) {
					log.fine("Executing SQL query: " + getUsersSQL);
				}
				resultSet = statement.executeQuery(getUsersSQL);
			} catch (SQLException sqle) {
				close();
				throw new TasteException(sqle);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasNext() {
			if (closed) {
				return false;
			}
			try {
				if (resultSet.isAfterLast()) {
					close();
					return false;
				} else {
					return true;
				}
			} catch (SQLException sqle) {
				log.log(Level.WARNING, "Unexpected exception while accessing ResultSet; continuing...", sqle);
				close();
				return false;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public User next() {

			if (closed) {
				throw new NoSuchElementException();
			}

			String currentUserID = null;
			final List<Preference> prefs = new ArrayList<Preference>();

			try {
				while (resultSet.next()) {
					final String userID = resultSet.getString(3);
					if (currentUserID == null) {
						currentUserID = userID;
					}
					// Did we move on to a new user?
					if (!userID.equals(currentUserID)) {
						// back up one row
						resultSet.previous();
						// we're done for now
						break;
					}
					// else add a new preference for the current user
					addPreference(resultSet, prefs);
				}
			} catch (SQLException sqle) {
				// No good way to handle this since we can't throw an exception
				log.log(Level.WARNING, "Exception while iterating over users", sqle);
				close();
				throw new NoSuchElementException("Can't retrieve more due to exception: " + sqle);
			}

			if (currentUserID == null) {
				// nothing left?
				throw new NoSuchElementException();
			}

			final User user = buildUser(currentUserID, prefs);

			transformUser(user);
			return user;
		}

		/**
		 * @throws UnsupportedOperationException
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void close() {
			closed = true;
			safeClose(resultSet, statement, connection);
		}

	}

	/**
	 * <p>An {@link java.util.Iterator} which returns {@link com.planetj.taste.model.Item}s from a {@link java.sql.ResultSet}. This is a useful
	 * way to iterate over all user data since it does not require all data to be read into memory
	 * at once. It does however require that the DB connection be held open. Note that this class will
	 * only release database resources after {@link #hasNext()} has been called and has returned false;
	 * callers should make sure to "drain" the entire set of data to avoid tying up database resources.</p>
	 *
	 * @author Sean Owen
	 */
	private final class ResultSetItemIterator implements Iterator<Item> {

		private final Connection connection;
		private final Statement statement;
		private final ResultSet resultSet;
		private boolean closed;

		ResultSetItemIterator() throws TasteException {
			try {
				connection = dataSource.getConnection();
				statement = connection.createStatement();
				if (log.isLoggable(Level.FINE)) {
					log.fine("Executing SQL query: " + getItemsSQL);
				}
				resultSet = statement.executeQuery(getItemsSQL);
			} catch (SQLException sqle) {
				close();
				throw new TasteException(sqle);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasNext() {
			if (closed) {
				return false;
			}
			try {
				if (resultSet.isAfterLast()) {
					close();
					return false;
				} else {
					return true;
				}
			} catch (SQLException sqle) {
				log.log(Level.WARNING, "Unexpected exception while accessing ResultSet; continuing...", sqle);
				close();
				return false;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public Item next() {

			if (closed) {
				throw new NoSuchElementException();
			}

			try {
				if (resultSet.next()) {
					return buildItem(resultSet.getString(1));
				} else {
					throw new NoSuchElementException();
				}
			} catch (SQLException sqle) {
				// No good way to handle this since we can't throw an exception
				log.log(Level.WARNING, "Exception while iterating over items", sqle);
				close();
				throw new NoSuchElementException("Can't retrieve more due to exception: " + sqle);
			}

		}

		/**
		 * @throws UnsupportedOperationException
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void close() {
			closed = true;
			safeClose(resultSet, statement, connection);
		}

	}

}
