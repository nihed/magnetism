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

package com.planetj.taste.ejb;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.ItemFilter;

import javax.ejb.EJBLocalObject;
import java.util.List;

/**
 * <p>Recommender EJB local component interface.</p>
 *
 * @author Sean Owen
 * @see RecommenderEJB
 * @see com.planetj.taste.recommender.Recommender
 */
public interface RecommenderEJBLocal extends EJBLocalObject {

	/**
	 * @see com.planetj.taste.recommender.Recommender#recommend(Object, int)
	 */
	List<Item> recommend(Object userID, int howMany) throws TasteException;

	/**
	 * @see com.planetj.taste.recommender.Recommender#recommend(Object, int, ItemFilter)
	 */
	List<Item> recommend(Object userID, int howMany, ItemFilter filter) throws TasteException;

	/**
	 * @see com.planetj.taste.recommender.Recommender#estimatePreference(Object, Object)
	 */
	double estimatePreference(Object userID, Object itemID) throws TasteException;

	/**
	 * @see com.planetj.taste.recommender.Recommender#setPreference(Object, Object, double)
	 */
	void setPreference(final Object userID, final Object itemID, final double value)
		throws TasteException;

	/**
	 * @see com.planetj.taste.recommender.Recommender#refresh()
	 */
	void refresh();

}
