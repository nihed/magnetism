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

import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.impl.model.GenericDataModel;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.User;

import java.util.Arrays;

/**
 * <p>Tests {@link PearsonCorrelation}.</p>
 *
 * @author Sean Owen
 */
abstract class CorrelationTestCase extends TasteTestCase {

	static DataModel getDataModel(final User... users) {
		return new GenericDataModel(Arrays.asList(users));
	}

}
