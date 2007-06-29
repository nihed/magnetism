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

package com.planetj.taste.impl.model.jdbc;

import com.planetj.taste.correlation.UserCorrelation;
import com.planetj.taste.impl.TasteTestCase;
import com.planetj.taste.impl.correlation.PearsonCorrelation;
import com.planetj.taste.impl.neighborhood.NearestNUserNeighborhood;
import com.planetj.taste.impl.recommender.GenericUserBasedRecommender;
import com.planetj.taste.impl.transforms.ZScore;
import com.planetj.taste.model.DataModel;
import com.planetj.taste.model.Preference;
import com.planetj.taste.neighborhood.UserNeighborhood;
import com.planetj.taste.recommender.Recommender;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Tests {@link com.planetj.taste.impl.model.jdbc.MySQLJDBCDataModel}.</p>
 *
 * <p>Requires a MySQL 4.x database running on the localhost, with a passwordless user named "mysql" available,
 * a database named "test", with a test data table whose name and columns match the defaults in
 * {@link MySQLJDBCDataModel}.</p>
 *
 * @author Sean Owen
 */
public final class MySQLJDBCDataModelTest extends TasteTestCase {

	private MysqlDataSource dataSource;

	@Override
	public void setUp() throws Exception {

		super.setUp();

		dataSource = new MysqlDataSource();
		dataSource.setUser("mysql");
		dataSource.setDatabaseName("test");
		dataSource.setServerName("localhost");

		final Connection connection = dataSource.getConnection();
		try {

			final PreparedStatement dropStatement =
			    connection.prepareStatement("DROP TABLE IF EXISTS " + MySQLJDBCDataModel.DEFAULT_PREFERENCE_TABLE);
			try {
				dropStatement.execute();
			} finally {
				dropStatement.close();
			}

			final PreparedStatement createStatement =
			    connection.prepareStatement("CREATE TABLE " + MySQLJDBCDataModel.DEFAULT_PREFERENCE_TABLE + " (" +
			                                MySQLJDBCDataModel.DEFAULT_USER_ID_COLUMN + " VARCHAR(11), " +
			                                MySQLJDBCDataModel.DEFAULT_ITEM_ID_COLUMN + " VARCHAR(11), " +
			                                MySQLJDBCDataModel.DEFAULT_PREFERENCE_COLUMN + " DOUBLE)");
			try {
				createStatement.execute();
			} finally {
				createStatement.close();
			}

			final PreparedStatement insertStatement =
			    connection.prepareStatement("INSERT INTO " + MySQLJDBCDataModel.DEFAULT_PREFERENCE_TABLE +
			                                " VALUES (?, ?, ?)");
			try {
				final String[] users =
					new String[]{"A123", "A123", "A123", "B234", "B234", "C345", "C345", "C345", "D456"};
				final String[] itemIDs = new String[]{"456", "789", "654", "123", "234", "789", "654", "123", "456"};
				final double[] preferences = new double[]{0.1, 0.6, 0.7, 1.0, 1.0, 0.6, 0.7, 1.0, 0.1};
				for (int i = 0; i < users.length; i++) {
					insertStatement.setString(1, users[i]);
					insertStatement.setString(2, itemIDs[i]);
					insertStatement.setDouble(3, preferences[i]);
					insertStatement.execute();
				}
			} finally {
				insertStatement.close();
			}

		} finally {
			connection.close();
		}
	}

	public void testDatabase() throws Exception {
		final DataModel model = new MySQLJDBCDataModel(dataSource);
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
		final DataModel model = new MySQLJDBCDataModel(dataSource);
		assertEquals("456", model.getItem("456").getID());
	}

	public void testPreferencesForItem() throws Exception {
		final DataModel model = new MySQLJDBCDataModel(dataSource);
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

	public void testAddTransform() throws Exception {
		final DataModel model = new MySQLJDBCDataModel(dataSource);
		// Make sure this doesn't throw an exception
		model.addTransform(new ZScore());
	}

	public void testSetPreference() throws Exception {
		final DataModel model = new MySQLJDBCDataModel(dataSource);
		model.setPreference("A123", "409", 2.0);
		assertEquals(2.0, model.getUser("A123").getPreferenceFor("409").getValue());
		model.setPreference("A123", "409", 1.0);
		assertEquals(1.0, model.getUser("A123").getPreferenceFor("409").getValue());
	}

}
