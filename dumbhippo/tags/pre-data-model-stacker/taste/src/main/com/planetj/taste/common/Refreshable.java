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

package com.planetj.taste.common;

/**
 * <p>Implementations of this interface have state that can be periodically refreshed. For example, an
 * implementation instance might contain some pre-computed information that should be periodically
 * refreshed. The {@link #refresh()} method triggers such a refresh.</p>
 *
 * <p>All Taste components implement this. In particular, {@link com.planetj.taste.recommender.Recommender}s do.
 * Callers may want to call {@link #refresh()} periodically to re-compute information throughout the system
 * and bring it up to date, though this operation may be expensive.</p>
 *
 * @author Sean Owen
 */
public interface Refreshable {

	/**
	 * Triggers "refresh" -- whatever that means -- of the implementation.
	 */
	void refresh();

}
