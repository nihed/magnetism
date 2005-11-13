package com.dumbhippo.hungry.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that gives the tests a bit more information that can't 
 * be pulled from purely black box viewing of the web pages
 * 
 * @author hp
 *
 */
public class CheatSheet {
	private static CheatSheet globalReadOnly;
	private static CheatSheet globalReadWrite;
	
	private Config config;
	private Connection dbConnection;
	private boolean readOnly;
	
	public static CheatSheet getReadOnly() {
		if (globalReadOnly == null)
			globalReadOnly = new CheatSheet(true);
		return globalReadOnly;
	}
	
	public static CheatSheet getReadWrite() {
		if (globalReadWrite == null)
			globalReadWrite = new CheatSheet(false);
		return globalReadWrite;
	}
	
	private CheatSheet(boolean readOnly) {
		this.readOnly = readOnly;
		
		config = Config.getDefault();
		// load jdbc driver
		String jdbcDriver = config.getValue(ConfigValue.DATABASE_DRIVER);
		try {
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Could not load JDBC driver " + jdbcDriver);
			System.exit(1);
		}	
	}
	
	private void fatalSqlException(SQLException e) {
		e.printStackTrace();
		System.err.println("DB connection messed up: " + e);
		System.exit(1);
	}
	
	private void failIfReadOnly() {
		if (readOnly)
			throw new IllegalStateException("attempt to do write operation on read-only " + getClass().getName());
	}
	
	public String getOneSampleUserId() {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT id FROM User LIMIT 1");
			ResultSet rs = statement.executeQuery();
			rs.next();
			return rs.getString("id");
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}
	}
	
	public Set<String> getAllUserIds() {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT id FROM User");
			ResultSet rs = statement.executeQuery();
			Set<String> ret = new HashSet<String>();
			while (rs.next()) {
				ret.add(rs.getString("id"));
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}		
	}
	
	private void nukeDatabaseRealDatabase() {
		try {
			DatabaseMetaData md = getConnection().getMetaData();
			ResultSet schemas = md.getSchemas();
			System.out.println("Going over each schema in database");
			while (schemas.next()) {
				String schemaName = schemas.getString("TABLE_SCHEM");
				String schemaCatalog = schemas.getString("TABLE_CATALOG");
				System.out.println("  Got schema " + schemaName + " catalog = " + schemaCatalog);
				ResultSet tables = md.getTables(schemaCatalog, schemaName, "%", null);
				while (tables.next()) {
					System.out.println("  Got table: " + 
							tables.getString("TABLE_CAT"));
				}
			}
		} catch (SQLException e) {
			fatalSqlException(e);
		}
	}
	
	private void nukeDatabaseToyDatabase() {
		try {
			PreparedStatement drop = getConnection().prepareStatement("drop database dumbhippo");
			PreparedStatement recreate = getConnection().prepareStatement("create database dumbhippo character set utf8 collate utf8_bin");
			
			drop.execute();
			recreate.execute();
			
		} catch (SQLException e) {
			fatalSqlException(e);
		}
	}
	
	public void nukeDatabase() {
		failIfReadOnly();
		
		nukeDatabaseToyDatabase();
	}
	
	private Connection getConnection() {		
		try {
			if (dbConnection != null && !dbConnection.isClosed())
				return dbConnection;
			
			dbConnection = DriverManager.getConnection(config.getValue(ConfigValue.DATABASE_URL),
					config.getValue(ConfigValue.DATABASE_USER),
					config.getValue(ConfigValue.DATABASE_PASSWORD));
			dbConnection.setReadOnly(readOnly);
			
			return dbConnection;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}
	}
}
