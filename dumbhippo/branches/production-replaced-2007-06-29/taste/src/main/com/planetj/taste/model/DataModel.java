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

package com.planetj.taste.model;

import com.planetj.taste.common.Refreshable;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.transforms.PreferenceTransform;

import java.util.List;

/**
 * <p>Implementations represent a repository of information about {@link User}s and their
 * associated {@link Preference}s for {@link Item}s.</p>
 *
 * @author Sean Owen
 */
public interface DataModel extends Refreshable {

	/**
	 * @return a {@link List} of all {@link User}s in the model, ordered by {@link User}
	 * @throws TasteException
	 */
	Iterable<User> getUsers() throws TasteException;

	/**
	 * @param id user ID
	 * @return {@link User} who has that ID
	 * @throws java.util.NoSuchElementException
	 *          if no such user exists
	 * @throws TasteException
	 */
	User getUser(Object id) throws TasteException;

	/**
	 * @return a {@link List} of all {@link Item}s in the model, order by {@link Item}
	 * @throws TasteException
	 */
	Iterable<Item> getItems() throws TasteException;

	/**
	 * @param id item ID
	 * @return {@link Item} that has that ID
	 * @throws java.util.NoSuchElementException
	 *          if no such user exists
	 * @throws TasteException
	 */
	Item getItem(Object id) throws TasteException;

	/**
	 * @param itemID item ID
	 * @return all existing {@link Preference}s expressed for that item, ordered by {@link User}
	 * @throws TasteException
	 */
	Iterable<Preference> getPreferencesForItem(Object itemID) throws TasteException;

	/**
	 * @return total number of {@link Item}s known to the model. This is generally the union
	 *         of all {@link Item}s preferred by at least one {@link User} but could include more.
	 * @throws TasteException
	 */
	int getNumItems() throws TasteException;

	/**
	 * @return total number of {@link User}s known to the model.
	 * @throws TasteException
	 */
	int getNumUsers() throws TasteException;

	/**
	 * <p>Adds a {@link PreferenceTransform} to this model.</p>
	 *
	 * @param preferenceTransform transform to add
	 * @throws TasteException
	 */
	void addTransform(PreferenceTransform preferenceTransform) throws TasteException;

	/**
	 * <p>Sets a particular preference (item plus rating) for a user.</p>
	 *
	 * @param userID user to set preference for
	 * @param itemID item to set preference for
	 * @param value preference value
	 * @throws TasteException
	 */
	void setPreference(Object userID, Object itemID, double value) throws TasteException;

}
