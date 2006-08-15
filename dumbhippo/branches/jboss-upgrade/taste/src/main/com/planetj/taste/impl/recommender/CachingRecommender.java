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
import com.planetj.taste.impl.common.Pair;
import com.planetj.taste.impl.common.SoftCache;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A {@link Recommender} which caches the results from another {@link Recommender} in memory.
 * Results are held by {@link SoftReference}s so that the JVM may reclaim memory from the recommendationCache
 * in low-memory situations.</p>
 *
 * @author Sean Owen
 */
public final class CachingRecommender implements Recommender {

	private static final Logger log = Logger.getLogger(CachingRecommender.class.getName());

	private final Recommender recommender;
	private final AtomicInteger maxHowMany;
	private final SoftCache<Object, List<RecommendedItem>> recommendationCache;
	private final SoftCache<Pair<?, ?>, Double> estimatedPrefCache;
	private final ReentrantLock refreshLock;

	public CachingRecommender(final Recommender recommender) {
		if (recommender == null) {
			throw new IllegalArgumentException();
		}
		this.recommender = recommender;
		this.maxHowMany = new AtomicInteger(1);
		this.recommendationCache = new SoftCache<Object, List<RecommendedItem>>(new RecommendationRetriever());
		this.estimatedPrefCache = new SoftCache<Pair<?, ?>, Double>(new EstimatedPrefRetriever());
		this.refreshLock = new ReentrantLock();
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany) throws TasteException {

		if (userID == null || howMany < 1) {
			throw new IllegalArgumentException();
		}

		synchronized (maxHowMany) {
			if (howMany > maxHowMany.get()) {
				maxHowMany.set(howMany);
			}
		}

		List<RecommendedItem> items = recommendationCache.get(userID);
		if (items.size() < howMany) {
			clear(userID);
			items = recommendationCache.get(userID);
		}

		if (items.size() > howMany) {
			return items.subList(0, howMany);
		} else {
			assert items.size() == howMany;
			return items;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany, final ItemFilter filter)
		throws TasteException {
		// Hmm, hard to recommendationCache this since the filter may change
		return recommender.recommend(userID, howMany, filter);
	}

	/**
	 * {@inheritDoc}
	 */
	public double estimatePreference(final Object userID, final Object itemID) throws TasteException {
		return estimatedPrefCache.get(new Pair<Object, Object>(userID, itemID));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreference(final Object userID, final Object itemID, final double value) throws TasteException {
		recommender.setPreference(userID, itemID, value);
		clear(userID);
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public DataModel getDataModel() {
		return recommender.getDataModel();
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
			recommender.refresh();
			clear();
		} finally {
			refreshLock.unlock();
		}
	}

	/**
	 * <p>Clears cached recommendations for the given user.</p>
	 *
	 * @param userID
	 */
	public void clear(final Object userID) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Clearing recommendations for user ID '" + userID + "'...");
		}
		recommendationCache.remove(userID);
	}

	/**
	 * <p>Clears all cached recommendations.</p>
	 */
	public void clear() {
		log.fine("Clearing all recommendations...");
		recommendationCache.clear();
	}

	private final class RecommendationRetriever implements SoftCache.Retriever<Object, List<RecommendedItem>> {
		public List<RecommendedItem> getValue(final Object userID) throws TasteException {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Retrieving new recommendations for user ID '" + userID + '\'');
			}
			return Collections.unmodifiableList(recommender.recommend(userID, maxHowMany.get()));
		}
	}

	private final class EstimatedPrefRetriever implements SoftCache.Retriever<Pair<?, ?>, Double> {
		public Double getValue(final Pair<?, ?> userAndItemIDs) throws TasteException {
			final Object userID = userAndItemIDs.getFirst();
			final Object itemID = userAndItemIDs.getSecond();
			if (log.isLoggable(Level.FINE)) {
				log.fine("Retrieving estimated preference for user ID '" + userID + "\' and item ID \'" +
				         itemID + '\'');
			}
			return recommender.estimatePreference(userID, itemID);
		}
	}

}
