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

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.common.SoftCache;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.User;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Computes a neigbhorhood consisting of all {@link User}s whose similarity to the
 * given {@link User} meets or exceeds a certain threshold. Similartiy is defined by the given
 * {@link UserCorrelation}.</p>
 *
 * @author Sean Owen
 */
public final class ThresholdUserNeighborhood extends AbstractUserNeighborhood {

	private static final Logger log = Logger.getLogger(ThresholdUserNeighborhood.class.getName());

	private final double threshold;
	private final SoftCache<Object, Collection<User>> cache;

	/**
	 * @param threshold       similarity threshold
	 * @param userCorrelation similarity metric
	 */
	public ThresholdUserNeighborhood(final double threshold,
	                                 final UserCorrelation userCorrelation,
	                                 final DataModel dataModel) {
		super(userCorrelation, dataModel);
		this.threshold = threshold;
		this.cache = new SoftCache<Object, Collection<User>>(new Retriever());
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Collection<User> getUserNeighborhood(final Object userID)
		throws TasteException {
		return cache.get(userID);
	}

	private final class Retriever implements SoftCache.Retriever<Object, Collection<User>> {

		public Collection<User> getValue(final Object userID) throws TasteException {
			if (log.isLoggable(Level.FINER)) {
				log.fine("Computing neighborhood around user ID '" + userID);
			}

			final DataModel dataModel = getDataModel();
			final User theUser = dataModel.getUser(userID);
			final List<User> neighborhood = new ArrayList<User>();
			final Iterator<User> users = dataModel.getUsers().iterator();
			final UserCorrelation userCorrelationImpl = getUserCorrelation();

			while (users.hasNext()) {
				final User user = users.next();
				if (!userID.equals(user.getID())) {
					final double theCorrelation = userCorrelationImpl.userCorrelation(theUser, user);
					if (!Double.isNaN(theCorrelation) && theCorrelation >= threshold) {
						neighborhood.add(user);
					}
				}
			}

			if (log.isLoggable(Level.FINER)) {
				log.fine("UserNeighborhood around user ID '" + userID + "' is: " +
				         neighborhood);
			}

			return Collections.unmodifiableList(neighborhood);
		}
	}

}
