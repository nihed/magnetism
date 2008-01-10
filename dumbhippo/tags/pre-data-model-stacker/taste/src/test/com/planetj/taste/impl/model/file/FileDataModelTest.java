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

package com.planetj.taste.impl.model.file;

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.transforms.CaseAmplification;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Item;
import com.planetj.taste.model.Preference;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Tests {@link com.planetj.taste.impl.model.file.FileDataModel}.</p>
 *
 * @author Sean Owen
 */
public final class FileDataModelTest extends TasteTestCase {

	private static final File testFile = new File("src/test/com/planetj/taste/impl/model/file/test1.txt");

	public void testFile() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		model.addTransform(new CaseAmplification(1.0)); // add no-op transform for testing
		final UserCorrelation userCorrelation = new PearsonCorrelation(model);
		final UserNeighborhood neighborhood = new NearestNUserNeighborhood(2, userCorrelation, model);
		final Recommender recommender = new GenericUserBasedRecommender(model, neighborhood);
		assertEquals(1, recommender.recommend("A123", 3).size());
		assertEquals(3, recommender.recommend("B234", 3).size());
		assertEquals(2, recommender.recommend("C345", 3).size());

		// Make sure this doesn't throw an exception
		model.refresh();
	}

	public void testItem() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		model.addTransform(new CaseAmplification(1.0)); // add no-op transform for testing
		assertEquals("456", model.getItem("456").getID());
	}

	public void testGetItems() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		model.addTransform(new CaseAmplification(1.0)); // add no-op transform for testing
		final Iterable<Item> items = model.getItems();
		assertNotNull(items);
		final Iterator<Item> it = items.iterator();
		assertNotNull(it);
		assertTrue(it.hasNext());
		assertEquals("123", it.next().getID());
		assertTrue(it.hasNext());
		assertEquals("234", it.next().getID());
		assertTrue(it.hasNext());
		assertEquals("456", it.next().getID());
		assertTrue(it.hasNext());
		assertEquals("654", it.next().getID());
		assertTrue(it.hasNext());
		assertEquals("789", it.next().getID());
		assertFalse(it.hasNext());
		try {
			it.next();
			fail("Should throw NoSuchElementException");
		} catch (NoSuchElementException nsee) {
			// good
		}
	}

	public void testPreferencesForItem() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		model.addTransform(new CaseAmplification(1.0)); // add no-op transform for testing
		final Iterable<Preference> prefs = model.getPreferencesForItem("456");
		assertNotNull(prefs);
		final Iterator<Preference> it = prefs.iterator();
		assertNotNull(it);
		assertTrue(it.hasNext());
		final Preference pref1 = it.next();
		assertEquals("A123", pref1.getUser().getID());
		assertEquals("456", pref1.getItem().getID());
		assertTrue(it.hasNext());
		final Preference pref2 = it.next();
		assertEquals("D456", pref2.getUser().getID());
		assertEquals("456", pref2.getItem().getID());
		assertFalse(it.hasNext());
		try {
			it.next();
			fail("Should throw NoSuchElementException");
		} catch (NoSuchElementException nsee) {
			// good
		}
	}

	public void testGetNumUsers() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		assertEquals(4, model.getNumUsers());
	}

	public void testSetPreference() throws Exception {
		final DataModel model = new FileDataModel(testFile);
		try {
			model.setPreference(null, null, 0.0);
			fail("Should have thrown UnsupportedOperationException");
		} catch (UnsupportedOperationException uoe) {
			// good
		}
	}

}
