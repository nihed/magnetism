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

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Owen
 */
abstract class AbstractRecommender implements Recommender {

	private static final Logger log = Logger.getLogger(AbstractRecommender.class.getName());

	private final DataModel dataModel;
	private final ReentrantLock refreshLock;

	AbstractRecommender(final DataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException();
		}
		this.dataModel = dataModel;
		this.refreshLock = new ReentrantLock();
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public List<RecommendedItem> recommend(final Object userID, final int howMany) throws TasteException {
		return recommend(userID, howMany, AllItemFilter.getInstance());
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreference(final Object userID, final Object itemID, final double value) throws TasteException {
		if (userID == null || itemID == null || Double.isNaN(value)) {
			throw new IllegalArgumentException();
		}
		if (log.isLoggable(Level.FINE)) {
			log.fine("Setting preference for user '" + userID + "', item '" + itemID + "', value " + value);
		}
		dataModel.setPreference(userID, itemID, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public DataModel getDataModel() {
		return dataModel;
	}

	/**
	 * {@inheritDoc}
	 */
	public void refresh() {
		if (refreshLock.isLocked()) {
			return;
		}
		refreshLock.lock();
		try {
			dataModel.refresh();
		} finally {
			refreshLock.unlock();
		}
	}

}
