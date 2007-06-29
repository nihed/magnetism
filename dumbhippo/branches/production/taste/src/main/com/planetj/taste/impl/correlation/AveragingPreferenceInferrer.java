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
import com.planetj.taste.impl.common.SoftCache;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Implementations of this interface compute an inferred preference for a {@link User} and an {@link Item}
 * that the user has not expressed any preference for. This might be an average of other preferences scores
 * from that user, for example. This technique is sometimes called "default voting".</p>
 *
 * @author Sean Owen
 */
public final class AveragingPreferenceInferrer implements PreferenceInferrer {

	private static final SoftCache.Retriever<User, Double> RETRIEVER =
		new SoftCache.Retriever<User, Double>() {
			public Double getValue(final User user) {
				return computeAveragePreferenceValue(user);
			}
		};

	private final SoftCache<User, Double> averagePreferenceValue;

	public AveragingPreferenceInferrer() {
		averagePreferenceValue = new SoftCache<User, Double>(RETRIEVER);
		refresh();
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public double inferPreference(final User user, final Item item) throws TasteException {
		if (user == null || item == null) {
			throw new IllegalArgumentException();
		}
		return averagePreferenceValue.get(user);
	}

	/**
	 * {@inheritDoc}
	 */
	public void refresh() {
		averagePreferenceValue.clear();
	}

	private static double computeAveragePreferenceValue(final User user) {
		int numItems = 0;
		double total = 0.0;
		for (final Preference pref : user.getPreferences()) {
			numItems++;
			total += pref.getValue();
		}
		if (numItems == 0) {
			return 0.0;
		}
		return total / (double) numItems;
	}

}
