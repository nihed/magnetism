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

import com.planetj.taste.model.Preference;

import java.util.Comparator;

/**
 * <p>{@link Comparator} that orders {@link com.planetj.taste.model.Preference}s from least preferred
 * to most preferred -- that is, in order of ascending value.</p>
 *
 * @author Sean Owen
 */
public final class ByValuePreferenceComparator implements Comparator<Preference> {

	/**
	 * {@inheritDoc}
	 */
	public int compare(final Preference p1, final Preference p2) {
		final double value1 = p1.getValue();
		final double value2 = p2.getValue();
		if (value1 < value2) {
			return -1;
		} else if (value1 > value2) {
			return 1;
		} else {
			return 0;
		}
	}

}
