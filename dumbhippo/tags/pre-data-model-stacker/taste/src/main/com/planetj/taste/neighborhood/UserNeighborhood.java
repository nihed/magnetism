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

package com.planetj.taste.neighborhood;

import com.planetj.taste.common.Refreshable;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.User;

import java.util.Collection;

/**
 * <p>Implementations of this interface compute a "neighborhood" of {@link User}s like a
 * given {@link User}. This neighborhood can be used to compute recommendations then.</p>
 *
 * @author Sean Owen
 */
public interface UserNeighborhood extends Refreshable {

	/**
	 * @param userID ID of user for which a neighborhood will be computed
	 * @return {@link Collection} of {@link User}s in the neighborhood
	 * @throws com.planetj.taste.common.TasteException if an error occurs while accessing data
	 */
	Collection<User> getUserNeighborhood(Object userID) throws TasteException;

}
