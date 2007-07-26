package com.dumbhippo.server.views;

import java.util.HashMap;
import java.util.Map;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.identity20.Guid;
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
	private Site site;
	
	private SystemViewpoint(Site site) {
		this.site = site;
	}
	
	static final private Map<Site,SystemViewpoint> instances;
	
	static {
		instances = new HashMap<Site,SystemViewpoint>();
		for (Site s : Site.values())
			instances.put(s, new SystemViewpoint(s));
	}
	
	// FIXME planning to take this out in my next commit
	static public SystemViewpoint getInstance() {
		return instances.get(Site.MUGSHOT);
	}
	
	/**
	 * Gets the system viewpoint singleton for the given site.
	 * 
	 * @return the global system viewpoint
	 */
	static public SystemViewpoint getInstance(Site site) {
		return instances.get(site);
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
	public Site getSite() {
		return site;
	}
}
