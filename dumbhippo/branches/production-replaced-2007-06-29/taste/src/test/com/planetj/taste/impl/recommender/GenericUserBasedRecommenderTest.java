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

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.User;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Tests {@link com.planetj.taste.impl.recommender.GenericUserBasedRecommender}.</p>
 *
 * @author Sean Owen, Paulo Magalhaes (pevm)
 */
public final class GenericUserBasedRecommenderTest extends RecommenderTestCase {

	public void testRecommender() throws Exception {
		final Recommender recommender = buildRecommender();
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1);
		assertNotNull(recommended);
		assertEquals(1, recommended.size());
		final RecommendedItem firstRecommended = recommended.get(0);
		assertEquals(new GenericItem<String>("1"), firstRecommended.getItem());
		assertEquals(0.8, firstRecommended.getValue());
	}

	public void testFilter() throws Exception {
		final Recommender recommender = buildRecommender();
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1, new DummyFilter());
		assertNotNull(recommended);
		assertEquals(0, recommended.size());
	}

	public void testEstimatePref() throws Exception {
		final Recommender recommender = buildRecommender();
		assertEquals(0.8, recommender.estimatePreference("test1", "1"));
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
		final UserCorrelation correlation = new PearsonCorrelation(dataModel);
		final UserNeighborhood neighborhood = new NearestNUserNeighborhood(1, correlation, dataModel);
		final Recommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood);;
		final List<RecommendedItem> recommended = recommender.recommend("test1", 1);
		assertNotNull(recommended);
		assertEquals(1, recommended.size());
		final RecommendedItem firstRecommended = recommended.get(0);
		// item one should be recommended because it has a greater rating/score
		assertEquals(new GenericItem<String>("2"), firstRecommended.getItem());
		assertEquals(0.3, firstRecommended.getValue(), EPSILON);
	}

	private static Recommender buildRecommender() {
		final DataModel dataModel = new GenericDataModel(getMockUsers());
		final UserCorrelation correlation = new PearsonCorrelation(dataModel);
		final UserNeighborhood neighborhood = new NearestNUserNeighborhood(1, correlation, dataModel);
		return new GenericUserBasedRecommender(dataModel, neighborhood);
	}

}
