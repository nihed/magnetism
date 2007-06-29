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

import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.impl.correlation.GenericItemCorrelation;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.User;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Tests {@link GenericItemBasedRecommender}.</p>
 *
 * @author Sean Owen, Paulo Magalhaes (pevm)
 */
public final class GenericItemBasedRecommenderTest extends RecommenderTestCase {

	public void testRecommender() throws Exception {
		final Recommender recommender = buildRecommender();
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1);
		assertNotNull(recommended);
		assertEquals(1, recommended.size());
		final RecommendedItem firstRecommended = recommended.get(0);
		assertEquals(new GenericItem<String>("1"), firstRecommended.getItem());
		assertEquals(0.0, firstRecommended.getValue());
	}

	public void testFilter() throws Exception {
		final Recommender recommender = buildRecommender();
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1, new DummyFilter());
		assertNotNull(recommended);
		assertEquals(0, recommended.size());
	}

	public void testEstimatePref() throws Exception {
		final Recommender recommender = buildRecommender();
		assertEquals(0.0, recommender.estimatePreference("test1", "1"));
	}

	/**
	 * Contributed test case that verifies fix for bug
	 *  <a href="http://sourceforge.net/tracker/index.php?func=detail&amp;aid=1396128&amp;group_id=138771&amp;atid=741665">
	 * 1396128</a>.
	 */
	public void testBestRating() throws Exception {
		final List<User> users = new ArrayList<User>(4);
		users.add(getUser("test1", 0.1, 0.3));
		users.add(getUser("test2", 0.2, 0.3, 0.3));
		users.add(getUser("test3", 0.4, 0.3, 0.5));
		users.add(getUser("test4", 0.7, 0.3, 0.8));
		final DataModel dataModel = new GenericDataModel(users);
		final Item item1 = new GenericItem<String>("0");
		final Item item2 = new GenericItem<String>("1");
		final Item item3 = new GenericItem<String>("2");
		final Collection<GenericItemCorrelation.ItemItemCorrelation> correlations =
			new ArrayList<GenericItemCorrelation.ItemItemCorrelation>(1);
		correlations.add(new GenericItemCorrelation.ItemItemCorrelation(item1, item2, 1.0));
		correlations.add(new GenericItemCorrelation.ItemItemCorrelation(item2, item3, 1.0));
		final ItemCorrelation correlation = new GenericItemCorrelation(correlations);
		final Recommender recommender = new GenericItemBasedRecommender(dataModel, correlation);;
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1);
		assertNotNull(recommended);
		assertEquals(1, recommended.size());
		final RecommendedItem firstRecommended = recommended.get(0);
		// item one should be recommended because it has a greater rating/score
		assertEquals(item3, firstRecommended.getItem());
		assertEquals(0.5, firstRecommended.getValue(), EPSILON);
	}

	private static Recommender buildRecommender() throws Exception {
		final DataModel dataModel = new GenericDataModel(getMockUsers());
		final Collection<GenericItemCorrelation.ItemItemCorrelation> correlations =
			new ArrayList<GenericItemCorrelation.ItemItemCorrelation>(1);
		final Item item1 = new GenericItem<String>("0");
		final Item item2 = new GenericItem<String>("1");
		correlations.add(new GenericItemCorrelation.ItemItemCorrelation(item1, item2, 1.0));
		final ItemCorrelation correlation = new GenericItemCorrelation(correlations);
		return new GenericItemBasedRecommender(dataModel, correlation);
	}

}
