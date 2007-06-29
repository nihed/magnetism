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

package com.planetj.taste.impl.recommender;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.impl.transforms.ZScore;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A simple {@link com.planetj.taste.recommender.Recommender} which uses a given
 * {@link com.planetj.taste.model.DataModel} and {@link com.planetj.taste.correlation.ItemCorrelation}
 * to produce recommendations. This class represents Taste's support for item-based recommenders.</p>
 *
 * <p>The {@link ItemCorrelation} is the most important point to discuss here. Item-based recommenders
 * are useful because they can take advantage of something to be very fast: they base their computations
 * on item correlation, not user correlation, and item correlation is relatively static. It can be
 * precomputed, instead of re-computed in real time.</p>
 *
 * <p>Thus it's strongly recommended that you use {@link com.planetj.taste.impl.correlation.GenericItemCorrelation}
 * with pre-computed correlations if you're going to use this class. You can use
 * {@link com.planetj.taste.impl.correlation.PearsonCorrelation} too, which computes correlations in real-time,
 * but will probably find this painfully slow for large amounts of data.</p>
 *
 * @author Sean Owen
 */
public final class GenericItemBasedRecommender extends AbstractRecommender {

	private static final Logger log = Logger.getLogger(GenericItemBasedRecommender.class.getName());

	private final ItemCorrelation correlation;
	private final ReentrantLock refreshLock;

	public GenericItemBasedRecommender(final DataModel dataModel, final ItemCorrelation correlation)
		throws TasteException {
		super(dataModel);
		if (correlation == null) {
			throw new IllegalArgumentException();
		}
		this.correlation = correlation;
		this.refreshLock = new ReentrantLock();

		if (log.isLoggable(Level.FINE)) {
			log.fine("Adding required Z-score transform to DataModel");
		}
		dataModel.addTransform(new ZScore());
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany, final ItemFilter filter)
		throws TasteException {

		if (userID == null || howMany < 1) {
			throw new IllegalArgumentException();
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Recommending items for user ID '" + userID + '\'');
		}

		final User theUser = getDataModel().getUser(userID);
		final int numPreferences = getNumPreferences(theUser);
		if (numPreferences == 0) {
			return Collections.emptyList();
		}

		// TODO: refactor this section somehow since it's so much like similar
		// sections in NearestNUserNeighborhood and GenericUserBasedRecommender
		final Set<Item> allItems = getAllOtherItems(theUser);
		final LinkedList<RecommendedItem> topItems = new LinkedList<RecommendedItem>();
		boolean full = false;
		for (final Item item : allItems) {
			if (item.isRecommendable() && filter.isAccepted(item)) {
				final double preference = doEstimatePreference(theUser, item, numPreferences);
				if (!full || preference > topItems.getFirst().getValue()) {
					final GenericRecommendedItem newItem = new GenericRecommendedItem(item, preference);
					int addAt = Collections.binarySearch(topItems, newItem);
					// See Collection.binarySearch() javadoc for an explanation of this:
					if (addAt < 0) {
						addAt = -addAt - 1;
					}
					topItems.add(addAt, newItem);
					if (full) {
						topItems.removeLast();
					} else if (topItems.size() > howMany) {
						full = true;
						topItems.removeLast();
					}
				}
			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Recommendations are: " + topItems);
		}
		return topItems;
	}

	/**
	 * {@inheritDoc}
	 */
	public double estimatePreference(final Object userID, final Object itemID) throws TasteException {
		final DataModel model = getDataModel();
		final User theUser = model.getUser(userID);
		final Preference actualPref = theUser.getPreferenceFor(itemID);
		if (actualPref != null) {
			return actualPref.getValue();
		}
		final Item item = model.getItem(itemID);
		final int numPreferences = getNumPreferences(theUser);
		return doEstimatePreference(theUser, item, numPreferences);
	}

	private double doEstimatePreference(final User theUser,
	                                    final Item item,
	                                    final int numPreferences) throws TasteException {
		double preference = 0.0;
		for (final Preference pref : theUser.getPreferences()) {
			preference += correlation.itemCorrelation(item, pref.getItem()) * pref.getValue();
		}
		return preference / (double) numPreferences;
	}

	private static int getNumPreferences(final User theUser) {
		int numPreferences = 0;
		for (final Iterator<Preference> it = theUser.getPreferences().iterator(); it.hasNext(); it.next()) {
			numPreferences++;
		}
		return numPreferences;
	}

	@NotNull
	private Set<Item> getAllOtherItems(final User theUser) throws TasteException {
		assert theUser != null;
		final Set<Item> allItems = new HashSet<Item>();
		for (final Item item : getDataModel().getItems()) {
			// If not already preferred by the user, add it
			if (theUser.getPreferenceFor(item.getID()) == null) {
				allItems.add(item);
			}
		}
		return allItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh() {
		if (refreshLock.isLocked()) {
			return;
		}
		refreshLock.lock();
		try {
			super.refresh();
			correlation.refresh();
		} finally {
			refreshLock.unlock();
		}
	}

}
