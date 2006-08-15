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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.planetj.taste.impl.common.ArrayIterator;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

/**
 * <p>A simple {@link User} which has simply an ID and some {@link Collection} of
 * {@link Preference}s.</p>
 *
 * @author Sean Owen
 */
public class GenericUser<K extends Comparable<K>> implements User, Serializable {

	private static final long serialVersionUID = -7674401701164830934L;
	
	private static final Comparator<Preference> BY_ITEM = new ByItemPreferenceComparator();
	private static final Preference[] NO_PREFS = new Preference[0];

	private final K id;
	private final Map<Object, Preference> data;
	// Use an array for maximum performance
	private final Preference[] values;

	public GenericUser(final K id, final Collection<Preference> preferences) {
		if (id == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		if (preferences == null || preferences.isEmpty()) {
			data = Collections.emptyMap();
			values = NO_PREFS;
		} else {
			data = new HashMap<Object, Preference>();
			values = preferences.toArray(new Preference[preferences.size()]);
			for (final Preference preference : values) {
				// Is this hacky?
				if (preference instanceof GenericPreference) {
					((GenericPreference) preference).setUser(this);
				}
				data.put(preference.getItem().getID(), preference);
			}
			Arrays.sort(values, BY_ITEM);
		}
	}

	@NotNull
	public K getID() {
		return id;
	}

	@Nullable
	public Preference getPreferenceFor(final Object itemID) {
		return data.get(itemID);
	}

	@NotNull
	public synchronized Iterable<Preference> getPreferences() {
		return new ArrayIterator<Preference>(values);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof GenericUser && ((GenericUser) obj).id.equals(id);
	}

	@Override
	@NotNull
	public String toString() {
		return "User[id:" + id + ']';
	}

	@SuppressWarnings({"unchecked"})
	public int compareTo(final User o) {
		return id.compareTo((K) o.getID());
	}

}
