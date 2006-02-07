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
public class LiveUser implements Ageable, Cloneable {
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
	
	private Hotness hotness;

	LiveUser(Guid userId) {
		this.hotness = Hotness.UNKNOWN;
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
}


