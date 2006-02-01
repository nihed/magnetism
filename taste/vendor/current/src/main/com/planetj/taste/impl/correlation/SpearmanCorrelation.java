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

package com.planetj.taste.impl.correlation;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.PreferenceInferrer;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.model.ByValuePreferenceComparator;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Like {@link PearsonCorrelation}, but compares relative ranking of preference values instead of preference
 * values themselves. That is, each {@link User}'s preferences are sorted and then assign a rank as their preference
 * value, with 1 being assigned to the least preferred item. Then the Pearson itemCorrelation of these rank values is
 * computed.</p>
 *
 * @author Sean Owen
 */
public final class SpearmanCorrelation implements UserCorrelation {

	private static final Comparator<Preference> BY_VALUE = new ByValuePreferenceComparator();

	private final UserCorrelation rankingUserCorrelation;
	private final ReentrantLock refreshLock;

	public SpearmanCorrelation(final DataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException();
		}
		this.rankingUserCorrelation = new PearsonCorrelation(dataModel);
		this.refreshLock = new ReentrantLock();
	}

	public SpearmanCorrelation(final UserCorrelation rankingUserCorrelation) {
		if (rankingUserCorrelation == null) {
			throw new IllegalArgumentException();
		}
		this.rankingUserCorrelation = rankingUserCorrelation;
		this.refreshLock = new ReentrantLock();		
	}

	/**
	 * {@inheritDoc}
	 */
	public double userCorrelation(final User user1, final User user2) throws TasteException {
		return rankingUserCorrelation.userCorrelation(new RankedPreferenceUser(user1),
		                                              new RankedPreferenceUser(user2));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreferenceInferrer(final PreferenceInferrer inferrer) {
		rankingUserCorrelation.setPreferenceInferrer(inferrer);
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
			rankingUserCorrelation.refresh();
		} finally {
			refreshLock.unlock();
		}
	}

	private static <K> List<K> iterableToList(final Iterable<K> iterable,
	                                          final Comparator<K> comparator) {
		final List<K> list = new ArrayList<K>();
		for (final K item : iterable) {
			list.add(item);
		}
		Collections.sort(list, comparator);
		return list;
	}

	private static final class RankedPreferenceUser implements User {

		private final User delegate;

		private RankedPreferenceUser(final User delegate) {
			this.delegate = delegate;
		}

		public Object getID() {
			return delegate.getID();
		}

		public Preference getPreferenceFor(final Object itemID) {
			throw new UnsupportedOperationException();
		}

		public Iterable<Preference> getPreferences() {
			return iterableToList(delegate.getPreferences(), BY_VALUE);
		}

		public int compareTo(final User o) {
			return delegate.compareTo(o);
		}

	}

}
