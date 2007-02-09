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

package com.planetj.taste.impl.model;

import com.planetj.taste.impl.common.EmptyIterable;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.transforms.PreferenceTransform;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <p>A simple {@link DataModel} which uses a given {@link List} of {@link User}s as
 * its data source. This implementation is mostly useful for small experiments and is not
 * recommended for contexts where performance is important.</p>
 *
 * @author Sean Owen
 */
public final class GenericDataModel implements DataModel, Serializable {

	private static final long serialVersionUID = -7102490459306690304L;

	private static final Iterable<Preference> NO_PREFS =
		new EmptyIterable<Preference>();

	private final List<User> users;
	private final Map<Object, User> userMap;
	private final List<Item> items;
	private final Map<Object, Item> itemMap;
	private final Map<Object, List<Preference>> preferenceForItems;

	public GenericDataModel(final Collection<? extends User> users) {
		if (users == null) {
			throw new IllegalArgumentException();
		}

		final List<User> usersCopy = new ArrayList<User>(users);
		Collections.sort(usersCopy);
		this.users = Collections.unmodifiableList(usersCopy);

		this.userMap = new HashMap<Object, User>();
		for (final User user : usersCopy) {
			userMap.put(user.getID(), user);
		}

		this.itemMap = new HashMap<Object, Item>();
		final Map<Object, List<Preference>> prefsForItems = new HashMap<Object, List<Preference>>();
		for (final User user : users) {
			for (final Preference preference : user.getPreferences()) {
				final Item item = preference.getItem();
				final Object itemID = item.getID();
				itemMap.put(itemID, item);
				List<Preference> prefs = prefsForItems.get(itemID);
				if (prefs == null) {
					prefs = new ArrayList<Preference>();
					prefsForItems.put(itemID, prefs);
				}
				prefs.add(preference);
			}
		}

		final List<Item> itemsCopy = new ArrayList<Item>(itemMap.values());
		Collections.sort(itemsCopy);
		final Comparator<Preference> comparator = new ByUserPreferenceComparator();
		for (final List<Preference> list : prefsForItems.values()) {
			Collections.sort(list, comparator);
		}

		this.items = Collections.unmodifiableList(itemsCopy);
		this.preferenceForItems = Collections.unmodifiableMap(prefsForItems);
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<User> getUsers() {
		return users;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NoSuchElementException if there is no such user
	 */
	@NotNull
	public User getUser(final Object id) {
		final User user = userMap.get(id);
		if (user == null) {
			throw new NoSuchElementException();
		} else {
			return user;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<Item> getItems() {
		return items;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NoSuchElementException if there is no such user
	 */
	@NotNull
	public Item getItem(final Object id) {
		final Item item = itemMap.get(id);
		if (item == null) {
			throw new NoSuchElementException();
		} else {
			return item;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterable<Preference> getPreferencesForItem(final Object itemID) {
		final List<Preference> prefs = preferenceForItems.get(itemID);
		return prefs == null ? NO_PREFS : prefs;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumItems() {
		return items.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumUsers() {
		return users.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTransform(final PreferenceTransform preferenceTransform) {
		if (preferenceTransform == null) {
			throw new IllegalArgumentException();
		}
		for (final User user : users) {
			preferenceTransform.transformPreferences(user);
		}
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
		// Does nothing
	}

}
