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

package com.planetj.taste.impl.recommender;

import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.ItemFilter;

/**
 * Simple filter that rejects item ID 1 only. Reasonable for testing.
 * 
 * @author Sean Owen
 */
final class DummyFilter implements ItemFilter {
	public boolean isAccepted(final Item item) {
		return !"1".equals(item.getID().toString());
	}
}
