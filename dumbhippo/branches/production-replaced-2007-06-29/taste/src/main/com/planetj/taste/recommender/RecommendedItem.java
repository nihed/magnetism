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

package com.planetj.taste.recommender;

import com.planetj.taste.model.Item;

/**
 * <p>Implementations encapsulate items that are recommended, and include
 * the {@link com.planetj.taste.model.Item} recommended and a value expressing
 * the strength of the preference.</p>
 *
 * @author Sean Owen
 */
public interface RecommendedItem extends Comparable<RecommendedItem> {

	/**
	 * @return the recommended {@link Item}
	 */
	Item getItem();

	/**
	 * <p>A value expressing the strength of the preference for the recommended
	 * {@link Item}. The range of the values depends on the implementation.
	 * Implementations must use larger values to express stronger preference.</p>
	 *
	 * @return strength of the preference
	 */
	double getValue();

}
