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

import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Tests {@link InverseUserFrequency}.</p>
 *
 * @author Sean Owen
 */
public final class InverseUserFrequencyTest extends TransformTestCase {

	public void testIUF() {
		final List<User> users = new ArrayList<User>(5);
		users.add(getUser("test1", 0.1));
		users.add(getUser("test2", 0.2, 0.3));
		users.add(getUser("test3", 0.4, 0.5, 0.6));
		users.add(getUser("test4", 0.7, 0.8, 0.9, 1.0));
		users.add(getUser("test5", 1.0, 1.0, 1.0, 1.0, 1.0));
		final GenericDataModel dummy = new GenericDataModel(users);
		final InverseUserFrequency iuf = new InverseUserFrequency(dummy);

		final User user = dummy.getUser("test5");
		iuf.transformPreferences(user);
		for (int i = 0; i < 5; i++) {
			assertEquals(Math.log(5.0 / (double) (5 - i)), user.getPreferenceFor(String.valueOf(i)).getValue());
		}

		// Make sure this doesn't throw an exception
		iuf.refresh();
	}

}
