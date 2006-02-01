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
 * <p>Applies "case amplification" to each preference score. This essentially makes big values bigger
 * and small values smaller by raising each score to a power. It could however be used to achieve the
 * opposite effect. Be careful of overflow when using this with large preference values; it might be
 * a good idea to apply {@link ZScore} first.</p>
 *
 * @author Sean Owen
 */
public final class CaseAmplification implements PreferenceTransform {

	private static final Logger log = Logger.getLogger(CaseAmplification.class.getName());

	private final double factor;

	/**
	 * <p>Creates a {@link CaseAmplification} transformation based on the given factor.</p>
	 *
	 * @param factor transformation factor
	 */
	public CaseAmplification(final double factor) {
		this.factor = factor;
	}

	/**
	 * <p>Each nonnegative preference value is set to <code>value<sup>factor</sup></code>; each
	 * negative value is set to <code>-value<sup>-factor</sup></code>.</p>
	 *
	 * @param user user whose preferences are to be transformed
	 */
	public void transformPreferences(final User user) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Preferences for user '" + user + "' before transform: " + user.getPreferences());
		}
		for (final Preference preference : user.getPreferences()) {
			final double value = preference.getValue();
			preference.setValue(value < 0.0 ? -Math.pow(-value, factor) : Math.pow(value, factor));
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
