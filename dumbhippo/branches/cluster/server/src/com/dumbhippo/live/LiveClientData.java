package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.Guid;

/**
 * The LiveClientData object represents current information about a
 * user that we are maintaining on behalf of connected clients, like
 * a list of active posts and a hotness level. (The distinction
 * and name are a little weird because we might want to eventually
 * expose some of this to the web interface, but it will work for
 * now.)
 * 
 * @author otaylor
 */
public class LiveClientData extends LiveObject {
	private int availableCount;

	// Externally interesting variables - update .equals when adding one of these
	
	private Hotness hotness;
	private List<Guid> activePosts;

	LiveClientData(Guid userId) {
		super(userId);
		this.availableCount = 0;
		this.hotness = Hotness.UNKNOWN;
		this.activePosts = new ArrayList<Guid>();
	}
		
	public void setAvailableCount(int availableCount) {
		this.availableCount = availableCount;
	}
	
	public int getAvailableCount() {
		return availableCount;
	}
	
	public boolean isAvailable() {
		return availableCount > 0;
	}
	
	public List<Guid> getActivePosts() {
		return activePosts;
	}

	public void setActivePosts(List<Guid> activePosts) {
		this.activePosts = new ArrayList<Guid>(activePosts);
	}

	public Hotness getHotness() {
		return hotness;
	}

	public void setHotness(Hotness hotness) {
		this.hotness = hotness;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// note this is only OK because LiveObject is abstract, if 
	// concrete LiveObject existed this would break transitivity
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof LiveClientData))
			return false;
		LiveClientData user = (LiveClientData) arg;
		return super.equals(user) 
				&& user.hotness.equals(hotness)
				&& user.activePosts.equals(activePosts);
	}
}
