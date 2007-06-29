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
import com.planetj.taste.model.User;

/**
 * <p>Implementations of this interface define a notion of itemCorrelation between two
 * {@link User}s. Implementations may return values from any range (e.g. 0.0 to 1.0);
 * the only restriction is that higher values must correspond to higher correlations.</p>
 *
 * @author Sean Owen
 * @see ItemCorrelation
 */
public interface UserCorrelation extends Refreshable {

	/**
	 * <p>Returns the "itemCorrelation", or degree of similarity, of two {@link User}s, based
	 * on the their preferences.</p>
	 *
	 * @param user1 first user
	 * @param user2 second user
	 * @return itemCorrelation between the two users
	 * @throws TasteException if an error occurs while accessing the data
	 */
	double userCorrelation(User user1, User user2) throws TasteException;

	/**
	 * <p>Attaches a {@link PreferenceInferrer} to the {@link UserCorrelation} implementation.</p>
	 *
	 * @param inferrer {@link PreferenceInferrer}
	 */
	void setPreferenceInferrer(PreferenceInferrer inferrer);

}
