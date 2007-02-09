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

import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.model.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A "generic" {@link ItemCorrelation} which takes a static list of precomputed {@link Item}
 * correlations and bases its responses on that alone. The values may have been precomputed
 * offline by another process, stored in a file, and then read and fed into an instance of this class.</p>
 *
 * <p>This is perhaps the best {@link ItemCorrelation} to use with
 * {@link com.planetj.taste.impl.recommender.GenericItemBasedRecommender}, since the point of item-based
 * recommenders is that they can take advantage of the fact that item similarity is relatively static,
 * can be precomputed, and then used in computation to gain a significant performance advantage.</p>
 *
 * @author Sean Owen
 */
public final class GenericItemCorrelation implements ItemCorrelation {

	private final Map<Item, Map<Item, Double>> correlationMaps;

	public GenericItemCorrelation(final Collection<ItemItemCorrelation> correlations) {
		final Map<Item, Map<Item, Double>> theCorrelationMaps = new HashMap<Item, Map<Item, Double>>(1009);
		for (final ItemItemCorrelation iic : correlations) {
			// Order them -- first key should be the "smaller" one
			final Item item1;
			final Item item2;
			if (iic.item1.compareTo(iic.item2) < 0) {
				item1 = iic.item1;
				item2 = iic.item2;
			} else {
				item1 = iic.item2;
				item2 = iic.item1;
			}
			Map<Item, Double> map = theCorrelationMaps.get(item1);
			if (map == null) {
				map = new HashMap<Item, Double>(1009);
				theCorrelationMaps.put(item1, map);
			}
			map.put(item2, iic.value);
		}
		correlationMaps = theCorrelationMaps;
	}


	/**
	 * {@inheritDoc}
	 */
	public double itemCorrelation(final Item item1, final Item item2) {
		final Item first;
		final Item second;
		if (item1.compareTo(item2) < 0) {
			first = item1;
			second = item2;
		} else {
			first = item2;
			second = item1;
		}
		final Map<Item, Double> nextMap = correlationMaps.get(first);
		if (nextMap == null) {
			return 0.0;
		}
		final Double correlation = nextMap.get(second);
		return correlation == null ? 0.0 : correlation;
	}

	/**
	 * {@inheritDoc}
	 */
	public void refresh() {
		// Do nothing
	}

	public static final class ItemItemCorrelation {
		private final Item item1;
		private final Item item2;
		private final double value;
		public ItemItemCorrelation(final Item item1, final Item item2, final double value) {
			this.item1 = item1;
			this.item2 = item2;
			this.value = value;
		}
	}

}
