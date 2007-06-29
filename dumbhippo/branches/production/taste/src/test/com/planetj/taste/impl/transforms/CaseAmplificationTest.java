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

import com.planetj.taste.model.User;

/**
 * <p>Tests {@link CaseAmplification}.</p>
 *
 * @author Sean Owen
 */
public final class CaseAmplificationTest extends TransformTestCase {

	public void testNoPref() {
		final User user = getUser("test");
		new CaseAmplification(1.0).transformPreferences(user);
		assertPrefsEquals(user);
	}

	public void testOnePref() {
		final User user = getUser("test", 1.0);
		new CaseAmplification(1.0).transformPreferences(user);
		assertPrefsEquals(user, 1.0);
	}

	public void testPositiveExp() {
		final User user = getUser("test", -1.0, 1.0, 3.0);
		new CaseAmplification(2.0).transformPreferences(user);
		assertPrefsEquals(user, -1.0, 1.0, 9.0);
	}

	public void testLessThanOneExp() {
		final User user = getUser("test", 0.0, 4.0, -4.0);
		new CaseAmplification(0.5).transformPreferences(user);
		assertPrefsEquals(user, 0.0, 2.0, -2.0);
	}

	public void testNegativeExp() {
		final User user = getUser("test", 0.0, 4.0, -4.0);
		new CaseAmplification(-2.0).transformPreferences(user);
		assertPrefsEquals(user, Double.POSITIVE_INFINITY, 0.0625, -0.0625);
	}

	public void testRefresh() {
		// Make sure this doesn't throw an exception
		new CaseAmplification(1.0).refresh();
	}

}
