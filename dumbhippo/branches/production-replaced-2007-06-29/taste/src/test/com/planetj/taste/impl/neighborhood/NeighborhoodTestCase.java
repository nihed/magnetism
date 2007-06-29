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

import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Owen
 */
abstract class NeighborhoodTestCase extends TasteTestCase {

	static List<User> getMockUsers() {
		final List<User> users = new ArrayList<User>(4);
		users.add(getUser("test1", 0.1));
		users.add(getUser("test2", 0.2));
		users.add(getUser("test3", 0.4));
		users.add(getUser("test4", 0.7));
		return users;
	}

}
