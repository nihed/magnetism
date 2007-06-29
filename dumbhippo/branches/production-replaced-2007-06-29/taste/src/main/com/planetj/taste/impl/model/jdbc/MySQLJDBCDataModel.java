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

import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.DataModel;

import javax.sql.DataSource;

/**
 * <p>A {@link DataModel} backed by a MySQL database and accessed via JDBC. It may work with other
 * JDBC databases. By default, this class assumes that there is a {@link DataSource} available under the
 * JNDI name "jdbc/taste", which gives access to a database with a "taste_preferences" table with the
 * following schema:</p>
 *
 * <table>
 * <tr><th>user_id</th><th>item_id</th><th>preference</th></tr>
 * <tr><td>ABC</td><td>123</td><td>0.9</td></tr>
 * <tr><td>ABC</td><td>456</td><td>0.1</td></tr>
 * <tr><td>DEF</td><td>123</td><td>0.2</td></tr>
 * <tr><td>DEF</td><td>789</td><td>0.3</td></tr>
 * </table>
 *
 * <p><code>user_id</code> must have a type compatible with the Java <code>String</code> type.
 * <code>item_id</code> must have a type compatible with the Java <code>String</code> type.
 * <code>preference</code> must have a type compatible with the Java <code>double</code> type.</p>
 *
 * <p>Given a {@link DataSource}, and the names of the table and the three columns, this class will return data in a
 * form suitable for Taste.</p>
 *
 * @author Sean Owen
 */
public class MySQLJDBCDataModel extends AbstractJDBCDataModel {

	public MySQLJDBCDataModel() throws TasteException {
		this(DEFAULT_DATASOURCE_NAME);
	}

	public MySQLJDBCDataModel(final String dataSourceName) throws TasteException {
		this(lookupDataSource(dataSourceName),
		     DEFAULT_PREFERENCE_TABLE,
		     DEFAULT_USER_ID_COLUMN,
		     DEFAULT_ITEM_ID_COLUMN,
		     DEFAULT_PREFERENCE_COLUMN);
	}

	public MySQLJDBCDataModel(final DataSource dataSource) {
		this(dataSource,
		     DEFAULT_PREFERENCE_TABLE,
		     DEFAULT_USER_ID_COLUMN,
		     DEFAULT_ITEM_ID_COLUMN,
		     DEFAULT_PREFERENCE_COLUMN);
	}

	public MySQLJDBCDataModel(final DataSource dataSource,
	                          final String preferenceTable,
	                          final String userIDColumn,
	                          final String itemIDColumn,
	                          final String preferenceColumn) {
		super(dataSource,
		      // getUserSQL
		      "SELECT " + itemIDColumn + ", " + preferenceColumn + " FROM " + preferenceTable +
		      " WHERE " + userIDColumn + "=? ORDER BY " + itemIDColumn,
		      // getNumItemsSQL
		      "SELECT COUNT(DISTINCT " + itemIDColumn + ") FROM " + preferenceTable,
		      // getNumUsersSQL
		      "SELECT COUNT(DISTINCT " + userIDColumn + ") FROM " + preferenceTable,
		      // setPreferenceSQL
		      "INSERT INTO " + preferenceTable + " SET " + userIDColumn + "=?, " + itemIDColumn +
		      "=?, " + preferenceColumn + "=? ON DUPLICATE KEY UPDATE " + preferenceColumn + "=?",
		      // getUsersSQL
		      "SELECT " + itemIDColumn + ", " + preferenceColumn + ", " + userIDColumn + " FROM " +
		      preferenceTable + " ORDER BY " + userIDColumn + ", " + itemIDColumn,
		      // getItemsSQL
		      "SELECT DISTINCT " + itemIDColumn + " FROM " + preferenceTable + " ORDER BY " + itemIDColumn,
		      // getItemSQL
		      "SELECT 1 FROM " + preferenceTable + " WHERE " + itemIDColumn + "=?",
		      // getPrefsForItemSQL
		      "SELECT " + preferenceColumn + ", " + userIDColumn + " FROM " +
		      preferenceTable + " WHERE " + itemIDColumn + "=? ORDER BY " + userIDColumn);
	}

}
