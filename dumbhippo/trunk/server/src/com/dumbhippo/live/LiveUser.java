package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.Guid;

/**
 * The LiveUser object represents current information about a
 * user. The set of information potentially exposed here includes 
 * both information that can be computed from the database, and 
 * information that is a function of the transient state of the
 * server. (Such as whether the user is available.)
 * 
 * @author otaylor
 */
public class LiveUser extends LiveObject {
	/**
	 * Get the User ID for which the LivePost object was created. 
	 * 
	 * @return the post ID for the post
	 */
	public Guid getUserId() {
		return userId;
	}
	
	/**********************************************************************/
	
	private int availableCount;
	
	// Externally interesting variables - update .equals when adding one of these
	private Guid userId;	
	private Hotness hotness;
	private List<Guid> activePosts;

	LiveUser(Guid userId) {
		this.hotness = Hotness.UNKNOWN;
		this.userId = userId;
		this.activePosts = new ArrayList<Guid>();
	}
		
	void setAvailableCount(int availableCount) {
		this.availableCount = availableCount;
	}
	
	int getAvailableCount() {
		return availableCount;
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
	
	public void discard() {
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof LiveUser))
			return false;
		LiveUser user = (LiveUser) arg;
		return user.userId.equals(userId) 
				&& user.hotness.equals(hotness)
				&& user.activePosts.equals(activePosts);
	}

	@Override
	public int hashCode() {
		return userId.hashCode();
	}
}


