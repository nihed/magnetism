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
 * <p>A {@link Preference} encapsulates an {@link Item} and a preference value, which
 * indicates the strength of the preference for it. {@link Preference}s are associated
 * to {@link User}s.</p>
 *
 * @author Sean Owen
 */
public interface Preference {

	/**
	 * @return {@link User} who prefers the {@link Item}
	 */
	User getUser();

	/**
	 * @return {@link Item} that is preferred
	 */
	Item getItem();

	/**
	 * @return strength of the preference for that item. Zero should indicate "no preference either way";
	 *         positive values indicate preference and negative values indicate dislike
	 */
	double getValue();

	/**
	 * Sets the strength of the preference for this item
	 *
	 * @param value new preference
	 */
	void setValue(double value);

}
