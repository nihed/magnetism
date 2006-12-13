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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Simple, fast {@link Iterator} for an array.</p>
 *
 * @author Sean Owen
 */
public final class ArrayIterator<T> implements Iterator<T>, Iterable<T> {

	private final T[] array;
	private int position;
	private final int max;

	/**
	 * <p>Creates an {@link ArrayIterator} over an entire array.</p>
	 *
	 * @param array
	 */
	public ArrayIterator(final T[] array) {
		this.array = array;
		this.position = 0;
		this.max = array.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		return position < max;
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public T next() {
		try {
			return array[position++];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			throw new NoSuchElementException();
		}
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterator<T> iterator() {
		return this;
	}

}
