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
import com.planetj.taste.model.Preference;
import com.planetj.taste.model.User;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * <p>A simple {@link Preference} encapsulating an {@link Item} and
 * preference value.</p>
 *
 * @author Sean Owen
 */
public class GenericPreference implements Preference, Serializable {

	private static final long serialVersionUID = -958267100740838726L;

	private User user;
	private final Item item;
	private double value;

	public GenericPreference(final User user, final Item item, final double value) {
		if (item == null || Double.isNaN(value)) {
			throw new IllegalArgumentException();
		}
		this.user = user;
		this.item = item;
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@NotNull
	public User getUser() {
		if (user == null) {
			throw new IllegalStateException("User was never set");
		}
		return user;
	}

	/**
	 * <p>Let this be set by {@link GenericUser} to avoid a circularity problem -- each
	 * wants a reference to the other in the constructor.</p>
	 *
	 * @param user
	 */
	void setUser(final User user) {
		if (user == null) {
			throw new IllegalArgumentException();
		}
		this.user = user;
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

	/**
	 * {@inheritDoc}
	 */
	public void setValue(final double value) {
		if (Double.isNaN(value)) {
			throw new IllegalArgumentException();
		}
		this.value = value;
	}

	@Override
	@NotNull
	public String toString() {
		return "Preference[item:" + item + ", value:" + value + ']';
	}

}
