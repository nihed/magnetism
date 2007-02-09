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

package com.planetj.taste.impl.neighborhood;

import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.correlation.PreferenceInferrer;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.User;

/**
 * @author Sean Owen
 */
final class DummyCorrelation implements UserCorrelation, ItemCorrelation {

	public double userCorrelation(final User user1, final User user2) {
		return 1.0 / Math.abs(user1.getPreferences().iterator().next().getValue() -
		                      user2.getPreferences().iterator().next().getValue());
	}

	public double itemCorrelation(final Item item1, final Item item2) {
		// Make up something wacky
		return (double) (item1.hashCode() - item2.hashCode());
	}

	public void setPreferenceInferrer(final PreferenceInferrer inferrer) {
		throw new UnsupportedOperationException();
	}

	public void refresh() {
		// do nothing
	}

}
