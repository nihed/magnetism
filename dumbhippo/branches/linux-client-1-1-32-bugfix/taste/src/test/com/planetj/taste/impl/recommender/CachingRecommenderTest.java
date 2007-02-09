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

import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Tests {@link com.planetj.taste.impl.recommender.CachingRecommender}.</p>
 *
 * @author Sean Owen
 */
public final class CachingRecommenderTest extends RecommenderTestCase {

	public void testRecommender() throws Exception {
		final AtomicInteger recommendCount = new AtomicInteger();
		final Recommender mockRecommender = new Recommender() {
			public List<RecommendedItem> recommend(final Object userID, final int howMany) {
				recommendCount.incrementAndGet();
				return Collections.<RecommendedItem>singletonList(
					new GenericRecommendedItem(new GenericItem<String>("1"), 1.0));
			}
			public List<RecommendedItem> recommend(final Object userID,
			                                       final int howMany,
			                                       final ItemFilter filter) {
				return recommend(userID, howMany);
			}
			public double estimatePreference(final Object userID, final Object itemID) {
				recommendCount.incrementAndGet();
				return 0.0;
			}
			public void setPreference(final Object userID, final Object itemID, final double value) {
				// do nothing
			}
			public DataModel getDataModel() {
				return null;
			}
			public void refresh() {
				// do nothing
			}
		};

		final Recommender cachingRecommender = new CachingRecommender(mockRecommender);
		cachingRecommender.recommend("1", 1);
		assertEquals(1, recommendCount.get());
		cachingRecommender.recommend("2", 1);
		assertEquals(2, recommendCount.get());
		cachingRecommender.recommend("1", 1);
		assertEquals(2, recommendCount.get());
		cachingRecommender.recommend("2", 1);
		assertEquals(2, recommendCount.get());
		cachingRecommender.refresh();
		cachingRecommender.recommend("1", 1);
		assertEquals(3, recommendCount.get());
		cachingRecommender.recommend("2", 1);
		assertEquals(4, recommendCount.get());
		cachingRecommender.recommend("3", 1);
		assertEquals(5, recommendCount.get());

		final ItemFilter filter = new DummyFilter();
		cachingRecommender.refresh();
		cachingRecommender.recommend("1", 1, filter);
		assertEquals(6, recommendCount.get());
		cachingRecommender.recommend("2", 1, filter);
		assertEquals(7, recommendCount.get());
		cachingRecommender.recommend("1", 1, filter);
		assertEquals(8, recommendCount.get());
		cachingRecommender.recommend("2", 1, filter);
		assertEquals(9, recommendCount.get());

		cachingRecommender.refresh();
		cachingRecommender.estimatePreference("test1", "1");
		assertEquals(10, recommendCount.get());
		cachingRecommender.estimatePreference("test1", "2");
		assertEquals(11, recommendCount.get());
		cachingRecommender.estimatePreference("test1", "2");
		assertEquals(11, recommendCount.get());
	}

}
