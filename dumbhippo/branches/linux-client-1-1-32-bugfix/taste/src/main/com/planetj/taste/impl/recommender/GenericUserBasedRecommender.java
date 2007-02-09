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
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A simple {@link Recommender} which uses a given {@link DataModel} and {@link UserNeighborhood}
 * to produce recommendations.</p>
 *
 * @author Sean Owen
 */
public final class GenericUserBasedRecommender extends AbstractRecommender {

	private static final Logger log = Logger.getLogger(GenericUserBasedRecommender.class.getName());

	private final UserNeighborhood neighborhood;
	private final ReentrantLock refreshLock;

	public GenericUserBasedRecommender(final DataModel dataModel, final UserNeighborhood neighborhood) {
		super(dataModel);
		if (neighborhood == null) {
			throw new IllegalArgumentException();
		}
		this.neighborhood = neighborhood;
		this.refreshLock = new ReentrantLock();
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
		final Collection<User> theNeighborhood = neighborhood.getUserNeighborhood(userID);

		if (log.isLoggable(Level.FINER)) {
			log.finer("UserNeighborhood is: " + neighborhood);
		}

		final Set<Item> allItems = getAllOtherItems(theNeighborhood, theUser);

		if (log.isLoggable(Level.FINER)) {
			log.finer("Items in neighborhood which user doesn't prefer already are: " + allItems);
		}

		final List<RecommendedItem> result = getTopItems(howMany, allItems, theNeighborhood, filter);

		if (log.isLoggable(Level.FINE)) {
			log.fine("Recommendations are: " + result);
		}

		return result;
	}

	@NotNull
	private static List<RecommendedItem> getTopItems(final int howMany,
	                                                 final Set<Item> allItems,
	                                                 final Collection<User> theNeighborhood,
	                                                 final ItemFilter filter) {
		final int neighborhoodSize = theNeighborhood.size();
		if (neighborhoodSize == 0) {
			return Collections.emptyList();
		}

		// TODO: refactor this section somehow since it's so much like similar
		// sections in NearestNUserNeighborhood and GenericItemBasedRecommender
		final LinkedList<RecommendedItem> topItems = new LinkedList<RecommendedItem>();
	
		// binary sort them by preference
		for (final Item item : allItems) {
			if (item.isRecommendable()) {
				final double preference = doEstimatePreference(theNeighborhood, item);
				final GenericRecommendedItem newItem = new GenericRecommendedItem(item, preference);
				int addAt = Collections.binarySearch(topItems, newItem);
				// See Collection.binarySearch() javadoc for an explanation of this:
				if (addAt < 0) {
					addAt = -addAt - 1;
				}
				topItems.add(addAt, newItem);			
			}
		}
		
		// now walk the sorted list from the front until we have howMany items
		//  that pass the isAccepted() test
		
		// note that we do this in a separate pass to minimize the number of calls
		//  to isAccepted, since it may (does) require a database query per call
		ArrayList<RecommendedItem> topHowManyItems = new ArrayList<RecommendedItem>(howMany);
		for (RecommendedItem recItem: topItems) {
			if (filter.isAccepted(recItem.getItem())) {
				topHowManyItems.add(recItem);
				if (topHowManyItems.size() >= howMany) {
					break;
				}
			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Recommendations are: " + topHowManyItems);
		}
		return topHowManyItems;
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
		final Collection<User> theNeighborhood = neighborhood.getUserNeighborhood(userID);
		final Item item = model.getItem(itemID);
		return doEstimatePreference(theNeighborhood, item);
	}

	private static double doEstimatePreference(final Collection<User> theNeighborhood, final Item item) {
		double preference = 0.0;
		for (final User user : theNeighborhood) {
			final Preference pref = user.getPreferenceFor(item.getID());
			if (pref != null) {
				preference += pref.getValue();
			}
		}
		return preference / (double) theNeighborhood.size();
	}

	@NotNull
	private static Set<Item> getAllOtherItems(final Collection<User> theNeighborhood, final User theUser) {
		final Set<Item> allItems = new HashSet<Item>();
		for (final User user : theNeighborhood) {
			for (final Preference preference : user.getPreferences()) {
				final Item item = preference.getItem();
				// If not already preferred by the user, add it
				if (theUser.getPreferenceFor(item.getID()) == null) {
					allItems.add(item);
				}
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
			neighborhood.refresh();
		} finally {
			refreshLock.unlock();
		}
	}

}
