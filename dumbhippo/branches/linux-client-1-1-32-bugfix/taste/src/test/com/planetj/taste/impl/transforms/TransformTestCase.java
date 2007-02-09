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

import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

/**
 * @author Sean Owen
 */
abstract class TransformTestCase extends TasteTestCase {

	static void assertPrefsEquals(final User user, final double... expected) {
		int i = 0;
		for (final Preference pref : user.getPreferences()) {
			assertEquals(expected[i], pref.getValue());
			i++;
		}
		assertEquals(expected.length, i);
	}
}
