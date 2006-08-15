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

package com.planetj.taste.model;

/**
 * <p>Implementations represent a user, who has preferences for {@link Item}s.</p>
 *
 * @author Sean Owen
 */
public interface User extends Comparable<User> {

	/**
	 * @return unique user ID
	 */
	Object getID();

	/**
	 * @param itemID ID of item to get the user's preference for
	 * @return user's {@link Preference} for that {@link Item}
	 */
	Preference getPreferenceFor(Object itemID);

	/**
	 * <p>Returns a sequence of {@link Preference}s for this {@link User} which can be iterated over.
	 * Note that the sequence <em>must</em> be "in order": ordered by {@link Item}.</p>
	 *
	 * @return a sequence of {@link Preference}s
	 */
	Iterable<Preference> getPreferences();

}
