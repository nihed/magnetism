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

package com.planetj.taste.transforms;

import com.planetj.taste.common.Refreshable;
import com.planetj.taste.model.User;

/**
 * <p>Implementations of this class represent a transformation of user's preference values.
 * Transformation might normalize, or exaggerate values to enhance the quality of
 * recommendations.</p>
 *
 * @author Sean Owen
 * @see com.planetj.taste.model.DataModel#addTransform(PreferenceTransform)
 */
public interface PreferenceTransform extends Refreshable {

	/**
	 * <p>Transforms all preference values for a user.</p>
	 *
	 * @param user user whose preferences are to be transformed
	 */
	void transformPreferences(User user);


}
