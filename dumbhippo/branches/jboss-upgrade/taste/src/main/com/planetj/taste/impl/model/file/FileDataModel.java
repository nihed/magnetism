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

package com.planetj.taste.impl.model.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.transforms.PreferenceTransform;

/**
 * <p>A {@link DataModel} backed by a comma-delimited file. This class assumes that each line of the
 * file contains a user ID, followed by item ID, followed by preferences value, separated by commas.
 * The item ID is assumed to be parseable as a <code>long</code>, and the preference value is assumed
 * to be parseable as a <code>double</code>.</p>
 *
 * <p>This class is not intended for use with very large amounts of data (over, say, a million rows). For
 * that, {@link com.planetj.taste.impl.model.jdbc.MySQLJDBCDataModel} and a database are more appropriate.
 * The file will be periodically reloaded if a change is detected.</p>
 *
 * @author Sean Owen
 */
public class FileDataModel implements DataModel {

	private static final Logger log = Logger.getLogger(FileDataModel.class.getName());

	private static final Timer timer = new Timer(true);
	private static final long RELOAD_CHECK_INTERVAL_MS = 60L * 1000L;

	private final File dataFile;
	private long lastModified;
	private boolean loaded;
	private DataModel delegate;
	private final List<PreferenceTransform> transforms;
	private final ReentrantLock refreshLock;
	private final ReentrantLock reloadLock;

	/**
	 * @param dataFile file containing preferences data
	 * @throws IOException if an error occurs while reading file data
	 * @throws FileNotFoundException if dataFile does not exist
	 */
	public FileDataModel(final File dataFile) throws IOException {

		if (dataFile == null) {
			throw new IllegalArgumentException();
		}
		if (!dataFile.exists() || dataFile.isDirectory()) {
			throw new FileNotFoundException(dataFile.toString());
		}

		log.info("Creating FileDataModel for file " + dataFile);

		this.dataFile = dataFile;
		this.lastModified = dataFile.lastModified();
		this.transforms = new ArrayList<PreferenceTransform>();
		this.refreshLock = new ReentrantLock();
		this.reloadLock = new ReentrantLock();

		// Schedule next refresh
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (loaded) {
					final long newModified = dataFile.lastModified();
					if (newModified > lastModified) {
						log.fine("File has changed; reloading...");
						lastModified = newModified;
						try {
							reload();
						} catch (IOException ioe) {
							log.log(Level.WARNING, "Error while reloading file", ioe);
						} catch (TasteException te) {
							log.log(Level.WARNING, "Error while reloading file", te);
						}
					}
				}
			}
		}, RELOAD_CHECK_INTERVAL_MS, RELOAD_CHECK_INTERVAL_MS);
	}

	private void reload() throws IOException, TasteException {

		if (reloadLock.isLocked()) {
			return;
		}

		reloadLock.lock();
		try {
			final Map<String, List<Preference>> data = new HashMap<String, List<Preference>>();

			log.info("Reading file info...");
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(dataFile));
				boolean notDone = true;
				while (notDone) {
					final String line = reader.readLine();
					if (line != null && line.length() > 0) {
						if (log.isLoggable(Level.FINE)) {
							log.fine("Read line: " + line);
						}
						processLine(line, data);
					} else {
						notDone = false;
					}
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ioe) {
						log.log(Level.WARNING, "Unexpected exception while closing file", ioe);
					}
				}
			}

			final List<User> users = new ArrayList<User>(data.size());
			for (final Map.Entry<String, List<Preference>> entries : data.entrySet()) {
				users.add(buildUser(entries.getKey(), entries.getValue()));
			}

			final DataModel newDelegate = new GenericDataModel(users);

			log.info("Applying transforms...");
			synchronized (transforms) {
				for (final PreferenceTransform transform : transforms) {
					newDelegate.addTransform(transform);
				}
			}

			delegate = newDelegate;

			loaded = true;

		} finally {
			reloadLock.unlock();
		}
	}

	private void processLine(final String line, final Map<String, List<Preference>> data) {
		final int commaOne = line.indexOf((int) ',');
		final int commaTwo = line.indexOf((int) ',', commaOne + 1);
		if (commaOne < 0 || commaTwo < 0) {
			throw new IllegalArgumentException("Bad line: " + line);
		}
		final String userID = line.substring(0, commaOne);
		final String itemID = line.substring(commaOne + 1, commaTwo);
		final double preferenceValue = Double.valueOf(line.substring(commaTwo + 1));
		List<Preference> prefs = data.get(userID);
		if (prefs == null) {
			prefs = new ArrayList<Preference>();
			data.put(userID, prefs);
		}
		final Item item = buildItem(itemID);
		if (log.isLoggable(Level.FINE)) {
			log.fine("Read item " + item + " for user ID " + userID);
		}
		prefs.add(buildPreference(null, item, preferenceValue));
	}

	private void checkLoaded() throws TasteException {
		if (!loaded) {
			try {
				reload();
			} catch (IOException ioe) {
				throw new TasteException(ioe);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<User> getUsers() throws TasteException {
		checkLoaded();
		return delegate.getUsers();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NoSuchElementException if there is no such user
	 */
	@NotNull
	public User getUser(final Object id) throws TasteException {
		checkLoaded();
		return delegate.getUser(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<Item> getItems() throws TasteException {
		checkLoaded();
		return delegate.getItems();
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Item getItem(final Object id) throws TasteException {
		checkLoaded();
		return delegate.getItem(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<Preference> getPreferencesForItem(final Object itemID) throws TasteException {
		checkLoaded();
		return delegate.getPreferencesForItem(itemID);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumItems() throws TasteException {
		checkLoaded();
		return delegate.getNumItems();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumUsers() throws TasteException {
		checkLoaded();
		return delegate.getNumUsers();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTransform(final PreferenceTransform preferenceTransform) throws TasteException {
		if (preferenceTransform == null) {
			throw new IllegalArgumentException();
		}
		checkLoaded();
		synchronized (transforms) {
			transforms.add(preferenceTransform);
		}
		delegate.addTransform(preferenceTransform);
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	public void setPreference(final Object userID, final Object itemID, final double value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void refresh() {
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
			try {
				reload();
			} catch (IOException ioe) {
				log.log(Level.WARNING, "Unexpected exception while refreshing", ioe);
			} catch (TasteException te) {
				log.log(Level.WARNING, "Unexpected exception while refreshing", te);				
			}
		} finally {
			refreshLock.unlock();
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

}
