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

package com.planetj.taste.impl.common;

import org.jetbrains.annotations.NotNull;

/**
 * A simple (ordered) pair of two objects. Elements may be null.
 *
 * @author Sean Owen
 */
public final class Pair<A, B> {

	private final A first;
	private final B second;

	public Pair(final A first, final B second) {
		this.first = first;
		this.second = second;
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null || !(other instanceof Pair)) {
			return false;
		}
		final Pair<?,?> otherPair = (Pair<?,?>) other;
		return isEqualOrNulls(first, otherPair.first) &&
	           isEqualOrNulls(second, otherPair.second);
	}

	private static boolean isEqualOrNulls(final Object obj1, final Object obj2) {
		return obj1 == null ? obj2 == null : obj1.equals(obj2);
	}

	@Override
	public int hashCode() {
		final int firstHash = hashCodeNull(first);
		// Flip top and bottom 16 bits; this makes the hash function probably different
		// for (a,b) versus (b,a)
		return (firstHash >>> 16 | firstHash << 16) ^ hashCodeNull(second);
	}

	private static int hashCodeNull(final Object obj) {
		return obj == null ? 0 : obj.hashCode();
	}

	@Override
	@NotNull
	public String toString() {
		return "(" + first + ',' + second + ')';
	}

}
