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

package com.planetj.taste.impl.neighborhood;

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.neighborhood.UserNeighborhood;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Contains methods and resources useful to all classes in this package.</p>
 *
 * @author Sean Owen
 */
abstract class AbstractUserNeighborhood implements UserNeighborhood {

	private final UserCorrelation userCorrelation;
	private final DataModel dataModel;

	AbstractUserNeighborhood(final UserCorrelation userCorrelation, final DataModel dataModel) {
		if (userCorrelation == null || dataModel == null) {
			throw new IllegalArgumentException();
		}
		this.userCorrelation = userCorrelation;
		this.dataModel = dataModel;
	}

	@NotNull
	final UserCorrelation getUserCorrelation() {
		return userCorrelation;
	}

	@NotNull
	final DataModel getDataModel() {
		return dataModel;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void refresh() {
		userCorrelation.refresh();
		dataModel.refresh();
	}

}
