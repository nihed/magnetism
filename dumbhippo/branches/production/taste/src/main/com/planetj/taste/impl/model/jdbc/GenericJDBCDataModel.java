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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>A generic {@link com.planetj.taste.model.DataModel} designed for use with other JDBC data sources;
 * one just specifies all necessary SQL queries to the constructor here. Optionally, the queries can
 * be specified from a {@link Properties} object, {@link File}, or {@link InputStream}. This class is
 * most appropriate when other existing implementations of {@link AbstractJDBCDataModel} are not suitable.
 * If you are using this class to support a major database, consider contributing a specialized implementation
 * of {@link AbstractJDBCDataModel} to the project for this database.</p>
 *
 * @author Sean Owen
 */
public class GenericJDBCDataModel extends AbstractJDBCDataModel {

	public static final String DATA_SOURCE_KEY = "dataSource";
	public static final String GET_USER_SQL_KEY = "getUserSQL";
	public static final String GET_NUM_USERS_SQL_KEY = "getNumUsersSQL";
	public static final String GET_NUM_ITEMS_SQL_KEY = "getNumItemsSQL";
	public static final String SET_PREFERENCE_SQL_KEY = "setPreferenceSQL";
	public static final String GET_USERS_SQL_KEY = "getUsersSQL";
	public static final String GET_ITEMS_SQL_KEY = "getItemsSQL";
	public static final String GET_ITEM_SQL_KEY = "getItemSQL";
	public static final String GET_PREFS_FOR_ITEM_SQL_KEY = "getPrefsForItemSQL";

	/**
	 * <p>Specifies all SQL queries in a {@link Properties} object. See the <code>*_KEY</code>
	 * constants in this class (e.g. {@link #GET_USER_SQL_KEY}) for a list of all keys which
	 * must map to a value in this object.</p>
	 *
	 * @param props {@link Properties} object containing values
	 * @throws TasteException if anything goes wrong during initialization
	 */
	public GenericJDBCDataModel(final Properties props) throws TasteException {
		super(lookupDataSource(props.getProperty(DATA_SOURCE_KEY)),
		      props.getProperty(GET_USER_SQL_KEY),
		      props.getProperty(GET_NUM_USERS_SQL_KEY),
		      props.getProperty(GET_NUM_ITEMS_SQL_KEY),
		      props.getProperty(SET_PREFERENCE_SQL_KEY),
		      props.getProperty(GET_USERS_SQL_KEY),
		      props.getProperty(GET_ITEMS_SQL_KEY),
		      props.getProperty(GET_ITEM_SQL_KEY),
		      props.getProperty(GET_PREFS_FOR_ITEM_SQL_KEY));
	}

	/**
	 * <p>See {@link #GenericJDBCDataModel(java.util.Properties)}. This constructor reads values
	 * from a file instead, as if with {@link Properties#load(InputStream)}. So, the file
	 * should be in standard Java properties file format -- containing <code>key=value</code> pairs,
	 * one per line.</p>
	 *
	 * @param propertiesFile
	 * @throws TasteException if anything goes wrong during initialization
	 */
	public GenericJDBCDataModel(final File propertiesFile) throws TasteException {
		this(getProperties(propertiesFile));
	}

	/**
	 * <p>See {@link #GenericJDBCDataModel(Properties)}. This constructor reads values
	 * from a resource available in the classpath, as if with {@link Class#getResourceAsStream(String)} and
	 * {@link Properties#load(InputStream)}. This is useful if your configuration file is, for example,
	 * packaged in a JAR file that is in the classpath.</p>
	 *
	 * @param resourcePath path to resource in classpath (e.g. "/com/foo/TasteSQLQueries.properties")
	 * @throws TasteException if anything goes wrong during initialization
	 */
	public GenericJDBCDataModel(final String resourcePath) throws TasteException {
		this(getProperties(GenericJDBCDataModel.class.getResourceAsStream(resourcePath)));
	}

	private static Properties getProperties(final File file) throws TasteException {
		try {
			return getProperties(new FileInputStream(file));
		} catch (FileNotFoundException fnfe) {
			throw new TasteException(fnfe);
		}
	}

	private static Properties getProperties(final InputStream is) throws TasteException {
		try {
			try {
				final Properties props = new Properties();
				props.load(is);
				return props;
			} finally {
				is.close();
			}
		} catch (IOException ioe) {
			throw new TasteException(ioe);
		}
	}

}
