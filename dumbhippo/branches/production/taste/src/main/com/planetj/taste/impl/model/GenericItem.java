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

package com.planetj.taste.impl.model;

import com.planetj.taste.model.Item;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * <p>An {@link Item} which has no data other than an ID.
 * This may be most useful for writing tests.</p>
 *
 * @author Sean Owen
 */
public class GenericItem<K extends Comparable<K>> implements Item, Serializable {

	private static final long serialVersionUID = -2603690367176230694L;
	
	private final K id;
	private final boolean recommendable;

	public GenericItem(final K id) {
		this(id, true);
	}

	public GenericItem(final K id, final boolean recommendable) {
		if (id == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		this.recommendable = recommendable;
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Object getID() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRecommendable() {
		return recommendable;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Item && ((Item) obj).getID().equals(id);
	}

	@Override
	@NotNull
	public String toString() {
		return "Item[id:" + id + ']';
	}

	@SuppressWarnings({"unchecked"})
	public int compareTo(final Item item) {
		return id.compareTo((K) item.getID());
	}

}
