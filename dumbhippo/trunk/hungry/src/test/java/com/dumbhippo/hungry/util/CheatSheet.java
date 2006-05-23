package com.dumbhippo.hungry.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.server.TestGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

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
	
	public Set<String> getSampleUserIds(int max) {
		// FIXME would improve the tests if we randomized the order so
		// when getting a fixed number we got different users each time
		try {
			// we only want users with an auth key, so we can log in 
			// as those users.
			String query =
			"SELECT HippoUser.id, Client.authKey "
				+ "FROM HippoUser "
				+ "INNER JOIN Account ON HippoUser.id = Account.owner_id "
				+ "LEFT JOIN Client ON Account.id = Client.account_id "
				+ "WHERE Client.authKey IS NOT NULL";
			if (max > 0)
				query = query + " LIMIT " + max;
			PreparedStatement statement =
				getConnection().prepareStatement(query);
			ResultSet rs = statement.executeQuery();
			Set<String> ret = new HashSet<String>();
			while (rs.next()) {
				String id = rs.getString("id");
				ret.add(id);
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null;
		}		
	}
	
	public String getOneSampleUserId() {
		Set<String> ret = getSampleUserIds(1);
		if (ret.size() == 0) {
			System.err.println("At this point in the tests, we need at least one user in the database (readonly tests don't work with an empty db)");
			throw new RuntimeException("no users in the db");
		}
		return ret.iterator().next();
	}
	
	public Set<String> getAllUserIds() {
		return getSampleUserIds(0); // 0 = unlimited
	}
	
	public String getUserAuthKey(String userId) {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT HippoUser.id, Client.authKey "
						+ "FROM HippoUser "
						+ "INNER JOIN Account ON HippoUser.id = Account.owner_id "
						+ "LEFT JOIN Client ON Account.id = Client.account_id "
						+ "WHERE HippoUser.id = ? LIMIT 1");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			String ret = null;
			while (rs.next()) {
				ret = rs.getString("authKey");
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null; // not reached
		}
	}
	
	public String getGroupId(String groupName) {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT HippoGroup.id "
						+ "FROM HippoGroup "
						+ "WHERE HippoGroup.name = ? LIMIT 1");
			statement.setString(1, groupName);
			ResultSet rs = statement.executeQuery();
			String ret = null;
			while (rs.next()) {
				ret = rs.getString("id");
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null; // not reached
		}
	}
	
	public String getUserId(String email) {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT ac.owner_id "
						+ "FROM AccountClaim ac, EmailResource er "
						+ "WHERE er.email = ? AND ac.resource_id = er.id LIMIT 1");
			statement.setString(1, email);
			ResultSet rs = statement.executeQuery();
			String ret = null;
			while (rs.next()) {
				ret = rs.getString("owner_id");
			}
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return null; // not reached
		}		
	}
	
	public int getNumberOfInvitations(String userId) {
		try {
			PreparedStatement statement =
				getConnection().prepareStatement("SELECT HippoUser.id, Account.invitations "
						+ "FROM HippoUser "
						+ "INNER JOIN Account ON HippoUser.id = Account.owner_id "
						+ "WHERE HippoUser.id = ?");
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			int ret = -1;
			while (rs.next()) {
				ret = rs.getInt("invitations");
			}
			if (ret < 0)
				throw new RuntimeException("no invitation count retrieved for user " + userId);
			return ret;
		} catch (SQLException e) {
			fatalSqlException(e);
			return 0; // not reached
		}
	}
	
	public void setNumberOfInvitations(String userId, int invites) {
		TestGlueRemote testGlue = EJBUtil.defaultLookup(TestGlueRemote.class);
		try {
			testGlue.setInvitations(userId, invites);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
			return null; // not reached
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
			// not reached
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
			return null; // not reached
		}
	}
}
