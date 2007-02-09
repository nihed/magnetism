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

/**
 * <p>An implementation of the cosine measure itemCorrelation.
 * Each {@link com.planetj.taste.model.User} X and Y is treated as a vector of preferences.
 * Their itemCorrelation is given as the dot product of the vectors divided by the product of
 * their magnitudes, which can be interpreted as the cosine of the angle between the vectors.</p>
 *
 * @author Sean Owen
 */
public final class CosineMeasureCorrelation extends AbstractVectorCorrelation {

	public CosineMeasureCorrelation(final DataModel dataModel) {
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
		final double normX = Math.sqrt(sumX2);
		final double normY = Math.sqrt(sumY2);
		final double denominator = normX * normY;
		return denominator == 0.0 ? 0.0 : sumXY / denominator;
	}

}
