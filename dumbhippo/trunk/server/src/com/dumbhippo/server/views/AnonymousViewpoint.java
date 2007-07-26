package com.dumbhippo.server.views;

import java.util.HashMap;
import java.util.Map;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;

/**
 * AnonymousViewpoint represents a anonymous public view onto
 * the system. Only data that should be visible to someone
 * not logged into the system can be accessed, and no modifications
 * can be made to the database.
 * 
 * @author otaylor
 */
public class AnonymousViewpoint extends Viewpoint {
	private Site site;
	
	private AnonymousViewpoint(Site site) {
		this.site = site;
	}
	
	static final private Map<Site,AnonymousViewpoint> instances;
	
	static {
		instances = new HashMap<Site,AnonymousViewpoint>();
		for (Site s : Site.values())
			instances.put(s, new AnonymousViewpoint(s));
	}
	
	// FIXME planning to take this out in my next commit
	static public AnonymousViewpoint getInstance() {
		return instances.get(Site.MUGSHOT);
	}
	
	/**
	 * Gets the anonymous viewpoint singleton for the given site.
	 * 
	 * @return the global anonymous viewpoint
	 */	
	static public AnonymousViewpoint getInstance(Site site) {
		return instances.get(site);
	}

	@Override
	public boolean isOfUser(User user) {
		return false;
	}
	
	@Override
	public String toString() {
		return "{AnonymousViewpoint}";
	}

	public void setSession(DMSession session) {
	}

	@Override
	public boolean canSeeFriendsOnly(Guid userId) {
		return false;
	}

	@Override
	public boolean canSeePrivate(Guid userId) {
		return false;
	}

	@Override
	public Site getSite() {
		return site;
	}
}
