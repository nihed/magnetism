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

import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import java.util.Collections;

/**
 * <p>Tests {@link CosineMeasureCorrelation}.</p>
 *
 * @author Sean Owen
 */
public final class CosineMeasureCorrelationTest extends CorrelationTestCase {

	public void testFullCorrelation1() throws Exception {
		final User user1 = getUser("test1", 3.0, -2.0);
		final User user2 = getUser("test2", 3.0, -2.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new CosineMeasureCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testFullCorrelation2() throws Exception {
		final User user1 = getUser("test1", 3.0, 3.0);
		final User user2 = getUser("test2", 3.0, 3.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new CosineMeasureCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testNoCorrelation1() throws Exception {
		final User user1 = getUser("test1", 3.0, -2.0);
		final User user2 = getUser("test2", -3.0, 2.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new CosineMeasureCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(-1.0, correlation, EPSILON);
	}

	public void testNoCorrelation2() throws Exception {
		final Preference pref1 = new GenericPreference(null, new GenericItem<String>("1"), 1.0);
		final GenericUser<String> user1 = new GenericUser<String>("test1", Collections.singletonList(pref1));
		final Preference pref2 = new GenericPreference(null, new GenericItem<String>("2"), 1.0);
		final GenericUser<String> user2 = new GenericUser<String>("test2", Collections.singletonList(pref2));
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new CosineMeasureCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(0.0, correlation, EPSILON);
	}

	public void testSimple() throws Exception {
		final User user1 = getUser("test1", 1, 2, 3);
		final User user2 = getUser("test2", 2, 5, 6);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new CosineMeasureCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(0.9944903161976938, correlation, EPSILON);
	}

	public void testFullItemCorrelation1() throws Exception {
		final User user1 = getUser("test1", 3.0, 3.0);
		final User user2 = getUser("test2", -2.0, -2.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation =
			new CosineMeasureCorrelation(dataModel).itemCorrelation(dataModel.getItem("0"), dataModel.getItem("1"));
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testFullItemCorrelation2() throws Exception {
		final User user1 = getUser("test1", 3.0, 3.0);
		final User user2 = getUser("test2", 3.0, 3.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation =
			new CosineMeasureCorrelation(dataModel).itemCorrelation(dataModel.getItem("0"), dataModel.getItem("1"));
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testNoItemCorrelation1() throws Exception {
		final User user1 = getUser("test1", 3.0, -3.0);
		final User user2 = getUser("test2", -2.0, 2.0);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation =
			new CosineMeasureCorrelation(dataModel).itemCorrelation(dataModel.getItem("0"), dataModel.getItem("1"));
		assertEquals(-1.0, correlation, EPSILON);
	}

	public void testNoItemCorrelation2() throws Exception {
		final Preference pref1 = new GenericPreference(null, new GenericItem<String>("1"), 1.0);
		final GenericUser<String> user1 = new GenericUser<String>("test1", Collections.singletonList(pref1));
		final Preference pref2 = new GenericPreference(null, new GenericItem<String>("2"), 1.0);
		final GenericUser<String> user2 = new GenericUser<String>("test2", Collections.singletonList(pref2));
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation =
			new CosineMeasureCorrelation(dataModel).itemCorrelation(dataModel.getItem("1"), dataModel.getItem("2"));
		assertEquals(0.0, correlation, EPSILON);
	}

	public void testSimpleItem() throws Exception {
		final User user1 = getUser("test1", 1, 2);
		final User user2 = getUser("test2", 2, 5);
		final User user3 = getUser("test3", 3, 6);
		final DataModel dataModel = getDataModel(user1, user2, user3);
		final double correlation =
			new CosineMeasureCorrelation(dataModel).itemCorrelation(dataModel.getItem("0"), dataModel.getItem("1"));
		assertEquals(0.9944903161976938, correlation, EPSILON);
	}

	public void testRefresh() {
		// Make sure this doesn't throw an exception
		new CosineMeasureCorrelation(getDataModel()).refresh();
	}

}
