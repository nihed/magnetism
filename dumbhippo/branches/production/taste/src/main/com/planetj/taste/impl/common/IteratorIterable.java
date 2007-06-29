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

/**
 * <p>Simple utility class that makes an {@link Iterator} {@link Iterable}
 * by returning the {@link Iterator} itself.</p>
 *
 * @author Sean Owen
 */
public final class IteratorIterable<T> implements Iterable<T> {

	private final Iterator<T> iterator;

	/**
	 * <p>Constructs an {@link IteratorIterable} for an {@link Iterator}.</p>
	 *
	 * @param iterator
	 */
	public IteratorIterable(final Iterator<T> iterator) {
		if (iterator == null) {
			throw new IllegalArgumentException("iterator is null");
		}
		this.iterator = iterator;
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Iterator<T> iterator() {
		return iterator;
	}

}
