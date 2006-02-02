package com.dumbhippo.live;

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
public class LiveUser implements Ageable {
	/**
	 * Get the User ID for which the LivePost object was created. 
	 * 
	 * @return the post ID for the post
	 */
	public Guid getUserId() {
		return userId;
	}
	
	/**********************************************************************/
	private Guid userId;
	private int availableCount;
	private int cacheAge;
	
	LiveUser(Guid userId) {
		this.userId = userId;
	}
		
	void setAvailableCount(int availableCount) {
		this.availableCount = availableCount;
	}
	
	int getAvailableCount() {
		return availableCount;
	}

	public int getCacheAge() {
		return cacheAge;
	}

	public void setCacheAge(int cacheAge) {
		this.cacheAge = cacheAge;
	}
	
	public void discard() {
	}
}


