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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Computes a neigbhorhood consisting of the nearest n {@link User}s to a given {@link User}.
 * "Nearest" is defined by the given {@link UserCorrelation}.</p>
 *
 * @author Sean Owen
 */
public final class NearestNUserNeighborhood extends AbstractUserNeighborhood {

	private static final Logger log = Logger.getLogger(NearestNUserNeighborhood.class.getName());

	private final SoftCache<Object, Collection<User>> cache;

	/**
	 * @param n neighborhood size
	 * @param userCorrelation nearness metric
	 */
	public NearestNUserNeighborhood(final int n,
	                                final UserCorrelation userCorrelation,
	                                final DataModel dataModel) {
		super(userCorrelation, dataModel);
		if (n < 1) {
			throw new IllegalArgumentException();
		}
		this.cache = new SoftCache<Object, Collection<User>>(new Retriever(n));
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

		private final int n;

		private Retriever(final int n) {
			this.n = n;
		}

		public Collection<User> getValue(final Object userID) throws TasteException {
			if (log.isLoggable(Level.FINER)) {
				log.fine("Computing neighborhood around user ID '" + userID + '\'');
			}

			final DataModel dataModel = getDataModel();
			final User theUser = dataModel.getUser(userID);
			final UserCorrelation userCorrelationImpl = getUserCorrelation();

			final LinkedList<UserCorrelationPair> queue = new LinkedList<UserCorrelationPair>();
			boolean full = false;
			for (final User user : dataModel.getUsers()) {
				if (!userID.equals(user.getID())) {
					final double theCorrelation = userCorrelationImpl.userCorrelation(theUser, user);
					if (!Double.isNaN(theCorrelation) && (!full || theCorrelation > queue.getFirst().theCorrelation)) {
						final UserCorrelationPair newPair = new UserCorrelationPair(user, theCorrelation);
						int addAt = Collections.binarySearch(queue, newPair);
						// See Collection.binarySearch() javadoc for an explanation of this:
						if (addAt < 0) {
							addAt = -addAt - 1;
						}
						queue.add(addAt, newPair);
						if (full) {
							queue.removeLast();
						} else if (queue.size() > n) {
							full = true;
							queue.removeLast();
						}
					}
				}
			}

			final List<User> neighborhood = new ArrayList<User>(queue.size());
			for (final UserCorrelationPair pair : queue) {
				neighborhood.add(pair.user);
			}

			if (log.isLoggable(Level.FINER)) {
				log.fine("UserNeighborhood around user ID '" + userID + "' is: " +
				         neighborhood);
			}

			return Collections.unmodifiableList(neighborhood);
		}
	}

	private static final class UserCorrelationPair implements Comparable<UserCorrelationPair> {

		private final User user;
		private final double theCorrelation;

		private UserCorrelationPair(final User user, final double theCorrelation) {
			this.user = user;
			this.theCorrelation = theCorrelation;
		}

		public int compareTo(final UserCorrelationPair otherPair) {
			final double otherCorrelation = otherPair.theCorrelation;
			return theCorrelation > otherCorrelation ? -1 : theCorrelation < otherCorrelation ? 1 : 0;
		}
	}

}
