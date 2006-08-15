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

package com.planetj.taste.impl.transforms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * <p>A simple, fast utility class that maps keys to counts.</p>
 *
 * @author Sean Owen
 */
final class Counters<T> {

	private final Map<T, MutableInteger> counts = new HashMap<T, MutableInteger>();

	void increment(final T key) {
		final MutableInteger count = counts.get(key);
		if (count == null) {
			final MutableInteger newCount = new MutableInteger();
			newCount.value = 1;
			counts.put(key, newCount);
		} else {
			count.value++;
		}
	}

	int getCount(final T key) {
		final MutableInteger count = counts.get(key);
		return count == null ? 0 : count.value;
	}

	int size() {
		return counts.size();
	}

	@NotNull
	Set<Map.Entry<T, MutableInteger>> getEntrySet() {
		return counts.entrySet();
	}

	static final class MutableInteger {

		private int value;

		int getValue() {
			return value;
		}
	}

}
