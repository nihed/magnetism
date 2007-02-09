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
 * <p>A simple interface whose implementations define "item filters", some
 * logic which accepts some items and rejects others. For example, an application
 * might define an implementation that only accepts items in a certain category
 * of items.</p>
 * 
 * @author Sean Owen
 * @see Recommender#recommend(Object, int, ItemFilter)
 */
public interface ItemFilter {

	/**
	 * @param item
	 * @return true iff this filter accepts the {@link Item}
	 */
	boolean isAccepted(Item item);

}
