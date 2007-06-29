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

import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;
import com.planetj.taste.transforms.PreferenceTransform;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Normalizes preference values for a {@link User} by converting them to
 * <a href="http://mathworld.wolfram.com/z-Score.html">"z-scores"</a>. This process
 * normalizes preference values to adjust for variation in mean and variance of a
 * user's preferences.</p>
 *
 * <p>Image two users, one who tends to rate every movie he/she sees four or five stars,
 * and another who uses the full one to five star range when assigning ratings. This
 * transform normalizes away the difference in scale used by the two users so that both
 * have a mean preference of 0.0 and a standard deviation of 1.0.</p>
 *
 * @author Sean Owen
 */
public final class ZScore implements PreferenceTransform {

	private static final Logger log = Logger.getLogger(ZScore.class.getName());

	/**
	 * <p>Calculates the mean and standard deviation of all preference values. Each preference value is then
	 * set to <code>(value - mean) / stdev</code>.
	 *
	 * @param user user whose preferences are to be transformed
	 */
	public void transformPreferences(final User user) {

		if (log.isLoggable(Level.FINER)) {
			log.finer("Preferences for user '" + user + "' before transform: " + user.getPreferences());
		}

		int num = 0;
		double sigma = 0.0;
		for (final Preference preference : user.getPreferences()) {
			num++;
			sigma += preference.getValue();
		}

		if (num > 0) {

			final double mean = sigma / (double) num;

			double sigmaSq = 0.0;
			for (final Preference preference : user.getPreferences()) {
				final double value = preference.getValue() - mean;
				sigmaSq += value * value;
			}

			final double stdev = Math.sqrt(sigmaSq / (double) num);

			if (stdev == 0.0) {
				for (final Preference preference : user.getPreferences()) {
					preference.setValue(0.0);
				}
			} else {
				for (final Preference preference : user.getPreferences()) {
					preference.setValue((preference.getValue() - mean) / stdev);
				}
			}

		}

		if (log.isLoggable(Level.FINER)) {
			log.finer("Preferences for user '" + user + "' after transform: " + user.getPreferences());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void refresh() {
		// do nothing
	}

}
