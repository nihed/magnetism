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

package com.planetj.taste.impl.correlation;

import com.planetj.taste.common.TasteException;
import com.planetj.taste.correlation.ItemCorrelation;
import com.planetj.taste.correlation.PreferenceInferrer;
import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A package-private class that unifies the implemenation of similar itemCorrelation
 * methods that compare two users as vectors of preferences. This class is quite central to
 * the Taste algorithms -- it holds the core {@link User} and {@link Item} correlation
 * logic used by most implementations.</p>
 *
 * @author Sean Owen
 * @see PearsonCorrelation
 * @see CosineMeasureCorrelation
 */
abstract class AbstractVectorCorrelation implements UserCorrelation, ItemCorrelation {

	private static final Logger log = Logger.getLogger(AbstractVectorCorrelation.class.getName());

	private DataModel dataModel;
	private PreferenceInferrer inferrer;

	AbstractVectorCorrelation(final DataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException();
		}
		this.dataModel = dataModel;
	}

	final DataModel getDataModel() {
		return dataModel;
	}

	final PreferenceInferrer getPreferenceInferrer() {
		return inferrer;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setPreferenceInferrer(final PreferenceInferrer inferrer) {
		if (inferrer == null) {
			throw new IllegalArgumentException();
		}
		this.inferrer = inferrer;
	}

	/**
	 * {@inheritDoc}
	 */
	public final double userCorrelation(final User user1, final User user2)
		throws TasteException {

		final Iterator<Preference> xIt = user1.getPreferences().iterator();
		final Iterator<Preference> yIt = user2.getPreferences().iterator();

		if (!(xIt.hasNext() && yIt.hasNext())) {
			return 0.0;
		}

		Preference xPref = xIt.next();
		Preference yPref = yIt.next();
		Item xIndex = xPref.getItem();
		Item yIndex = yPref.getItem();

		double sumX = xPref.getValue();
		double sumX2 = xPref.getValue() * xPref.getValue();
		double sumY = yPref.getValue();
		double sumY2 = yPref.getValue() * yPref.getValue();
		double sumXY = 0.0;

		final boolean hasInferrer = inferrer != null;

		try {
			while (true) {

				final int compare = xIndex.compareTo(yIndex);

				if (compare == 0) {
					// Both users expressed a preference for the item
					sumXY += xPref.getValue() * yPref.getValue();
				} else if (hasInferrer) {
					// Only one user expressed a preference, but infer the other one's preference and tally
					// as if the other user expressed that preference
					if (compare < 0) {
						// X has a value; infer Y's
						final double inferred = inferrer.inferPreference(user2, xIndex);
						sumXY += xPref.getValue() * inferred;
						sumY += inferred;
						sumY2 += inferred * inferred;
					} else {
						// compare > 0
						// Y has a value; infer X's
						final double inferred = inferrer.inferPreference(user1, yIndex);
						sumXY += inferred * yPref.getValue();
						sumX += inferred;
						sumX2 += inferred * inferred;
					}
				}

				if (compare < 0) {
					xPref = xIt.next();
					xIndex = xPref.getItem();
					final double x = xPref.getValue();
					sumX += x;
					sumX2 += x * x;
				} else {
					yPref = yIt.next();
					yIndex = yPref.getItem();
					final double y = yPref.getValue();
					sumY += y;
					sumY2 += y * y;
				}

			}
		} catch (NoSuchElementException nsee) {
			// It's ugly to terminate the loop with an exception from Iterator.next(),
			// but the overhead of calling hasNext() repeatedly in this performance-critical
			// code is too much
		}

		if (xIt.hasNext()) {
			while (xIt.hasNext()) {
				xPref = xIt.next();
				xIndex = xPref.getItem();
				if (xIndex.compareTo(yIndex) == 0) {
					sumXY += xPref.getValue() * yPref.getValue();
				}
				final double x = xPref.getValue();
				sumX += x;
				sumX2 += x * x;
			}
		} else {
			while (yIt.hasNext()) {
				yPref = yIt.next();
				yIndex = yPref.getItem();
				if (xIndex.compareTo(yIndex) == 0) {
					sumXY += xPref.getValue() * yPref.getValue();
				}
				final double y = yPref.getValue();
				sumY += y;
				sumY2 += y * y;
			}
		}

		final int size = dataModel.getNumItems();
		final double result = computeResult(size, sumXY, sumX, sumY, sumX2, sumY2);
		if (log.isLoggable(Level.FINER)) {
			log.finer("UserCorrelation between " + user1 + " and " + user2 + " is " + result);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public final double itemCorrelation(final Item item1, final Item item2) throws TasteException {

		final Iterator<Preference> xIt = dataModel.getPreferencesForItem(item1.getID()).iterator();
		final Iterator<Preference> yIt = dataModel.getPreferencesForItem(item2.getID()).iterator();

		if (!(xIt.hasNext() && yIt.hasNext())) {
			return 0.0;
		}

		Preference xPref = xIt.next();
		Preference yPref = yIt.next();
		User xIndex = xPref.getUser();
		User yIndex = yPref.getUser();

		double sumX = xPref.getValue();
		double sumX2 = xPref.getValue() * xPref.getValue();
		double sumY = yPref.getValue();
		double sumY2 = yPref.getValue() * yPref.getValue();
		double sumXY = 0.0;

		try {
			while (true) {

				final int compare = xIndex.compareTo(yIndex);

				if (compare == 0) {
					// Both users expressed a preference for the item
					sumXY += xPref.getValue() * yPref.getValue();
				}

				if (compare < 0) {
					xPref = xIt.next();
					xIndex = xPref.getUser();
					final double x = xPref.getValue();
					sumX += x;
					sumX2 += x * x;
				} else {
					yPref = yIt.next();
					yIndex = yPref.getUser();
					final double y = yPref.getValue();
					sumY += y;
					sumY2 += y * y;
				}

			}
		} catch (NoSuchElementException nsee) {
			// See comments above on why this is ugly but OK
		}

		if (xIt.hasNext()) {
			while (xIt.hasNext()) {
				xPref = xIt.next();
				xIndex = xPref.getUser();
				if (xIndex.compareTo(yIndex) == 0) {
					sumXY += xPref.getValue() * yPref.getValue();
				}
				final double x = xPref.getValue();
				sumX += x;
				sumX2 += x * x;
			}
		} else {
			while (yIt.hasNext()) {
				yPref = yIt.next();
				yIndex = yPref.getUser();
				if (xIndex.compareTo(yIndex) == 0) {
					sumXY += xPref.getValue() * yPref.getValue();
				}
				final double y = yPref.getValue();
				sumY += y;
				sumY2 += y * y;
			}
		}

		final int size = dataModel.getNumUsers();
		final double result = computeResult(size, sumXY, sumX, sumY, sumX2, sumY2);
		if (log.isLoggable(Level.FINER)) {
			log.finer("UserCorrelation between " + item1 + " and " + item2 + " is " + result);
		}
		return result;
	}

	/**
	 * <p>Several subclasses in this package implement this method to actually compute the correlation
	 * from figures computed over users or items.</p>
	 *
	 * @param n total number of users or items
	 * @param sumXY sum of product of user/item preference values, over all items/users prefererred by
	 *  both users/items
	 * @param sumX sum of all user/item preference values, over the first item/user
	 * @param sumY sum of all user/item preference values, over the second item/user
	 * @param sumX2 sum of the square of user/item preference values, over the first item/user
	 * @param sumY2 sum of the square of the user/item preference values, over the second item/user
	 * @return correlation value
	 */
	abstract double computeResult(int n, double sumXY, double sumX, double sumY, double sumX2, double sumY2);

	/**
	 * {@inheritDoc}
	 */
	public final void refresh() {
		dataModel.refresh();
		if (inferrer != null) {
			inferrer.refresh();
		}
	}

}
