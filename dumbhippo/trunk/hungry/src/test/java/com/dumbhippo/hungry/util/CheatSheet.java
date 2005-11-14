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
	
	public String getUserAuthKey(String userId) {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT User.id, Client.authKey "
						+ "FROM User "
						+ "INNER JOIN Account ON User.id = Account.owner_id "
						+ "LEFT JOIN Client ON Account.id = Client.account_id "
						+ "WHERE User.id = ? LIMIT 1");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			String ret = null;
			while (rs.next()) {
				ret = rs.getString("authKey");
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}
	}
	
	public Set<String> getUnacceptedInvitationAuthKeys() {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT Token.authKey " 
						+ "FROM Token "
						+ "INNER JOIN InvitationToken ON Token.id = InvitationToken.id " 
						+ "WHERE (InvitationToken.resultingPerson_id IS NULL AND InvitationToken.viewed = 0)");
			
			Set<String> ret = new HashSet<String>();
		
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				ret.add(rs.getString("authKey"));
			}

			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}
	}
	
	public void nukeDatabase() {
		failIfReadOnly();
	
		try {
			DatabaseMetaData md = getConnection().getMetaData();
			ResultSet tables = md.getTables(null, null, null, null);
			
			while (tables.next()) {
				String table = tables.getString("TABLE_NAME");
				System.out.println("  Emptying table: " + table);
				PreparedStatement truncate = getConnection().prepareStatement("truncate " + table);
				truncate.execute();
			}
		} catch (SQLException e) {
			fatalSqlException(e);
		}
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
