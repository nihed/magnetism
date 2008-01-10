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

package com.planetj.taste.impl;

import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.impl.model.GenericPreference;
import com.planetj.taste.impl.model.GenericUser;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Owen
 */
public abstract class TasteTestCase extends TestCase {

	/** "Close enough" value for floating-point comparisons. */
	public static final double EPSILON = 0.000000000000001;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// make sure we always show all log output during tests
		setLogLevel(Level.FINEST);
	}

	protected static void setLogLevel(final Level level) {
		final Logger log = Logger.getLogger("com.planetj.taste.impl");
		log.setLevel(level);
		for (final Handler handler : log.getHandlers()) {
			handler.setLevel(level);
		}
	}

	public static User getUser(final String userID, final double... values) {
		final List<Preference> prefs = new ArrayList<Preference>(values.length);
		int i = 0;
		for (final double value : values) {
			prefs.add(new GenericPreference(null, new GenericItem<String>(String.valueOf(i)), value));
			i++;
		}
		return new GenericUser<String>(userID, prefs);
	}

}
