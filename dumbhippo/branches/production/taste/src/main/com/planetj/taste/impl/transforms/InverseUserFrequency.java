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

package com.planetj.taste.impl.transforms;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.transforms.PreferenceTransform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Implements an "inverse user frequency" transformation, which boosts preference values for items for which few
 * users have expressed a preference, and reduces preference values for items for which many users have expressed
 * a preference. The idea is that these "rare" {@link Item}s are more useful in deciding how similar two users'
 * tastes are, and so should be emphasized in other calculatioons. This idea is mentioned in
 * <a href="ftp://ftp.research.microsoft.com/pub/tr/tr-98-12.pdf">Empirical Analysis of Predictive Algorithms for
 * Collaborative Filtering</a>.</p>
 *
 * @author Sean Owen
 */
public final class InverseUserFrequency implements PreferenceTransform {

	private static final Logger log = Logger.getLogger(InverseUserFrequency.class.getName());

	private final DataModel dataModel;
	private final AtomicReference<Map<Item, Double>> iufFactors;
	private boolean loaded;

	/**
	 * <p>Creates a {@link InverseUserFrequency} transformation.</p>
	 *
	 * @param dataModel
	 */
	public InverseUserFrequency(final DataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException();
		}
		this.dataModel = dataModel;
		this.iufFactors = new AtomicReference<Map<Item, Double>>(new HashMap<Item, Double>());
		refresh();
	}

	/**
	 * <p>If there are <code>n</code> users in the {@link DataModel}, then this method multiplies the
	 * preference value for each {@link Item} by <code>log(n/m), where <code>m</code> is the number of users
	 * who have expressed a preference for that {@link Item}.</p>
	 *
	 * @param user user whose preferences are to be transformed
	 */
	public void transformPreferences(final User user) {

		if (log.isLoggable(Level.FINER)) {
			log.finer("Preferences for user '" + user + "' before transform: " + user.getPreferences());
		}

		final Map<Item, Double> currentIufFactors = iufFactors.get();

		for (final Preference preference : user.getPreferences()) {
			final Item item = preference.getItem();
			final Double factor = currentIufFactors.get(item);
			if (factor != null) {
				// Should always contain the key, since there's an entry for each item that any User has
				// a preference for, but want to make sure in case we're out of sync with the DataModel now
				preference.setValue(preference.getValue() * factor);
			}
		}

		if (log.isLoggable(Level.FINER)) {
			log.finer("Preferences for user '" + user + "' after transform: " + user.getPreferences());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void refresh() {

		int numUsers = 0;
		final Counters<Item> itemPreferenceCounts = new Counters<Item>();
		try {
			for (final User user : dataModel.getUsers()) {
				for (final Preference preference : user.getPreferences()) {
					itemPreferenceCounts.increment(preference.getItem());
				}
				numUsers++;
			}
		} catch (TasteException dme) {
			log.log(Level.WARNING, "Unable to refresh", dme);
		}

		final Map<Item, Double> newIufFactors =
			new HashMap<Item, Double>(1 + (4 * itemPreferenceCounts.size()) / 3, 0.75f);
		for (final Map.Entry<Item, Counters.MutableInteger> entry : itemPreferenceCounts.getEntrySet()) {
			final double factor = Math.log((double) numUsers / (double) entry.getValue().getValue());
			newIufFactors.put(entry.getKey(), factor);
		}
		iufFactors.set(Collections.unmodifiableMap(newIufFactors));
	}

}
