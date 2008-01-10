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
 * <p>Tests {@link ZScore}.</p>
 *
 * @author Sean Owen
 */
public final class ZScoreTest extends TransformTestCase {

	public void testNoPref() {
		final User user = getUser("test");
		new ZScore().transformPreferences(user);
		assertPrefsEquals(user);
	}

	public void testOnePref() {
		final User user = getUser("test", 1.0);
		new ZScore().transformPreferences(user);
		assertPrefsEquals(user, 0.0);
	}

	public void testAllSame() {
		final User user = getUser("test", 1.0, 1.0, 1.0);
		new ZScore().transformPreferences(user);
		assertPrefsEquals(user, 0.0, 0.0, 0.0);
	}

	public void testStdev() {
		final User user = getUser("test", -1.0, -2.0);
		new ZScore().transformPreferences(user);
		assertPrefsEquals(user, 1.0, -1.0);
	}

	public void testRefresh() {
		// Make sure this doesn't throw an exception
		new ZScore().refresh();
	}

}
