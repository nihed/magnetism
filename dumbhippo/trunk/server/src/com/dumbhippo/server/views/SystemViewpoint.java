package com.dumbhippo.server.views;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.dm.BlockDMOKey;

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
	
	static final private SystemViewpoint instance = new SystemViewpoint();
	
	/**
	 * Gets the system viewpoint singleton. Its Site is always Site.NONE 
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

	public void setSession(DMSession session) {
	}

	@Override
	public boolean canSeeFriendsOnly(Guid userId) {
		return true;
	}

	@Override
	public boolean canSeePrivate(Guid userId) {
		return true;
	}

	@Override
	public boolean canSeeContact(Guid contactId) {
		return true;
	}	
	
	@Override
	public boolean canSeeBlock(BlockDMOKey blockKey) {
		return true;
	}
	
	@Override
	public boolean canSeePost(Guid postId) {
		return true;
	}
	
	// the SystemViewpoint is never relative to Site.GNOME or Site.MUGSHOT
	@Override
	public Site getSite() {
		return Site.NONE;
	}
}
