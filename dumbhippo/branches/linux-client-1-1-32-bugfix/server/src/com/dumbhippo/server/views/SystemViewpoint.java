package com.dumbhippo.server.views;

import com.dumbhippo.persistence.User;

/**
 * SystemViewpoint represents the systems view of the database.
 * This is an omniscient and omnipotent view; no access controls 
 * are applied. Be very careful about using this as it could easily 
 * result in information leakage to the system in a read-only situation
 * or vulnerabities in a read-write situation. Just like the
 * root user on a Unix system, the System viewpoint should be
 * used in as small portions of code as possible; in general
 * a good rule of thumb is never to create a SystemViewpoint
 * when you have a UserViewpoint; if a user is allowed to do
 * something, make the checks at the point of database access
 * take that into account.
 * 
 * @author otaylor
 */
public class SystemViewpoint extends Viewpoint {
	private SystemViewpoint() {
	}
	
	static private final SystemViewpoint instance = new SystemViewpoint();
	
	/**
	 * Gets the system viewpoint singleton.
	 * 
	 * @return the global system viewpoint
	 */
	static public SystemViewpoint getInstance() {
		return instance;
	}

	@Override
	public boolean isOfUser(User user) {
		return false;
	}

	@Override
	public String toString() {
		return "{SystemViewpoint}";
	}
}
