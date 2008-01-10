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

import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.User;

/**
 * <p>Tests {@link SpearmanCorrelation}.</p>
 *
 * @author Sean Owen
 */
public final class SpearmanCorrelationTest extends CorrelationTestCase {

	public void testFullCorrelation1() throws Exception {
		final User user1 = getUser("test1", 1, 2, 3);
		final User user2 = getUser("test2", 1, 2, 3);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new SpearmanCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testFullCorrelation2() throws Exception {
		final User user1 = getUser("test1", 1, 2, 3);
		final User user2 = getUser("test2", 4, 5, 6);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new SpearmanCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(1.0, correlation, EPSILON);
	}

	public void testSimple() throws Exception {
		final User user1 = getUser("test1", 1, 2, 3);
		final User user2 = getUser("test2", 3, 2, 1);
		final DataModel dataModel = getDataModel(user1, user2);
		final double correlation = new SpearmanCorrelation(dataModel).userCorrelation(user1, user2);
		assertEquals(-4.5, correlation, EPSILON);
	}

	public void testRefresh() {
		// Make sure this doesn't throw an exception
		new SpearmanCorrelation(getDataModel()).refresh();
	}

}
