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
import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.impl.model.GenericItem;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.User;

/**
 * <p>Tests {@link AveragingPreferenceInferrer}.</p>
 *
 * @author Sean Owen
 */
public final class AveragingPreferenceInferrerTest extends TasteTestCase {

	public void testInferrer() throws TasteException {
		final User user1 = getUser("test1", 3.0, -2.0, 5.0);
		final Item item = new GenericItem<String>("3");
		final AveragingPreferenceInferrer inferrer = new AveragingPreferenceInferrer();
		final double inferred = inferrer.inferPreference(user1, item);
		assertEquals(2.0, inferred);
	}

}
