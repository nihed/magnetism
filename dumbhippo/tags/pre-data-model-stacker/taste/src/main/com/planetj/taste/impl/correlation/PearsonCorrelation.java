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

import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.User;

/**
 * <p>An implementation of the Pearson itemCorrelation. For {@link User}s X and Y, the following values
 * are calculated:</p>
 *
 * <ul>
 * <li>sumX: sum of all X's preference values</li>
 * <li>sumX2: sum of the square of all X's preference values</li>
 * <li>sumY: sum of all Y's preference values</li>
 * <li>sumY2: sum of the square of all Y's preference values</li>
 * <li>sumXY: sum of the product of X and Y's preference value for all items for which both
 * X and Y express a preference</li>
 * </ul>
 *
 * <p>The itemCorrelation is then:
 *
 * <p><code>(size*sumXY - sumX*sumY) /
 * sqrt((size*sumX2 - sumX<sup>2</sup>) * (size*sumY2 - sumY<sup>2</sup>))</code></p>
 *
 * <p>where <code>size</code> is the number of {@link Item}s in the {@link DataModel}.</p>
 *
 * @author Sean Owen
 */
public final class PearsonCorrelation extends AbstractVectorCorrelation {

	public PearsonCorrelation(final DataModel dataModel) {
		super(dataModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	double computeResult(final int n,
	                     final double sumXY,
	                     final double sumX,
	                     final double sumY,
	                     final double sumX2,
	                     final double sumY2) {
		final double xTerm = Math.sqrt((double) n * sumX2 - sumX * sumX);
		final double yTerm = Math.sqrt((double) n * sumY2 - sumY * sumY);
		final double denominator = xTerm * yTerm;
		if (denominator == 0.0) {
			// One or both parties has -all- the same ratings;
			// can't really say much correlation under this measure
			return 0.0;
		}
		return ((double) n * sumXY - sumX * sumY) / denominator;
	}

}
