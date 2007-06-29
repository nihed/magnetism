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

import com.planetj.taste.common.Refreshable;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;

import java.util.List;

/**
 * <p>Implementations of this interface can recommend {@link Item}s for a
 * {@link com.planetj.taste.model.User}. Implementations will likely take advantage of several
 * classes in other packages here to compute this.</p>
 *
 * @author Sean Owen
 */
public interface Recommender extends Refreshable {

	/**
	 * @param userID  user for which recommendations are to be computed
	 * @param howMany desired number of recommendations
	 * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly
	 *         recommend to least
	 * @throws TasteException if an error occurs while accessing the {@link com.planetj.taste.model.DataModel}
	 */
	List<RecommendedItem> recommend(Object userID, int howMany) throws TasteException;

	/**
	 * @param userID  user for which recommendations are to be computed
	 * @param howMany desired number of recommendations
	 * @param filter filter to apply; only {@link Item}s accepted by the filter will be returned
	 * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly
	 *         recommend to least
	 * @throws TasteException if an error occurs while accessing the {@link com.planetj.taste.model.DataModel}
	 */
	List<RecommendedItem> recommend(Object userID, int howMany, ItemFilter filter) throws TasteException;

	/**
	 * @param userID user ID whose preference is to be estimated
	 * @param itemID item ID to estimate preference for
	 * @return an estimated preference if the user has not expressed a preference for the item, or else
	 *  the user's actual preference for the item
	 * @throws TasteException if an error occurs while accessing the {@link com.planetj.taste.model.DataModel}
	 */
	double estimatePreference(Object userID, Object itemID) throws TasteException;

	/**
	 * @param userID user to set preference for
	 * @param itemID item to set preference for
	 * @param value preference value
	 * @throws com.planetj.taste.common.TasteException
	 *          if an error occurs while accessing the {@link com.planetj.taste.model.DataModel}
	 */
	void setPreference(Object userID, Object itemID, double value) throws TasteException;

	/**
	 * @return {@link DataModel} used by this {@link Recommender}
	 */
	DataModel getDataModel();

}
