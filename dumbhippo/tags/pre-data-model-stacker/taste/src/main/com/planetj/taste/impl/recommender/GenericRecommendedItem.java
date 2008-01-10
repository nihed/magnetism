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

package com.planetj.taste.impl.recommender;

import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.RecommendedItem;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * <p>A simple implementation of {@link RecommendedItem}.</p>
 *
 * @author Sean Owen
 */
public final class GenericRecommendedItem implements RecommendedItem, Serializable {

	private static final long serialVersionUID = -1518879793101509780L;

	private final Item item;
	private final double value;

	/**
	 * @param item
	 * @param value
	 * @throws IllegalArgumentException if item is null or value is NaN
	 */
	public GenericRecommendedItem(final Item item, final double value) {
		if (item == null) {
			throw new IllegalArgumentException("item is null");
		}
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException("value is NaN");
		}
		this.item = item;
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public Item getItem() {
		return item;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getValue() {
		return value;
	}

	@Override
	@NotNull
	public String toString() {
		return "RecommendedItem[item:" + item + ", value:" + value + ']';
	}

	/**
	 * Defines a natural ordering from most-preferred item (highest value) to least-preferred.
	 *
	 * @param other
	 * @return 1, -1, 0 as this value is less than, greater than or equal to the other's value
	 */
	public int compareTo(final RecommendedItem other) {
		final double otherValue = other.getValue();
		if (value < otherValue) {
			return 1;
		} else if (value > otherValue) {
			return -1;
		} else {
			return 0;
		}
	}

}
