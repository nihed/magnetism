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

package com.planetj.taste.correlation;

import com.planetj.taste.common.Refreshable;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.Item;

/**
 * <p>Implementations of this interface define a notion of itemCorrelation between two
 * {@link com.planetj.taste.model.Item}s. Implementations may return values from any range (e.g. 0.0 to 1.0);
 * the only restriction is that higher values must correspond to higher correlations.</p>
 *
 * @author Sean Owen
 * @see UserCorrelation
 */
public interface ItemCorrelation extends Refreshable {

	/**
	 * <p>Returns the "itemCorrelation", or degree of similarity, of two {@link com.planetj.taste.model.Item}s, based
	 * on the preferences that {@link com.planetj.taste.model.User}s have expressed for the items.</p>
	 *
	 * @param item1
	 * @param item2
	 * @return itemCorrelation between the {@link com.planetj.taste.model.Item}s
	 * @throws TasteException if an error occurs while accessing the data
	 */
	double itemCorrelation(Item item1, Item item2) throws TasteException;

}
