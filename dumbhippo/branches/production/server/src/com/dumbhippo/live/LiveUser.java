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
	private int availableCount;
	
	// Externally interesting variables - update .equals when adding one of these
	private Hotness hotness;
	private List<Guid> activePosts;
	private int groupCount;
	private int sentPostsCount;

	LiveUser(Guid userId) {
		super(userId);
		this.hotness = Hotness.UNKNOWN;
		this.activePosts = new ArrayList<Guid>();
		this.availableCount = 0;
		this.groupCount = 0;
		this.sentPostsCount = 0;
	}
		
	public void setAvailableCount(int availableCount) {
		this.availableCount = availableCount;
	}
	
	public int getAvailableCount() {
		return availableCount;
	}

	public boolean isOnline() {
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
		if (!(arg instanceof LiveUser))
			return false;
		LiveUser user = (LiveUser) arg;
		return super.equals(user) 
				&& user.hotness.equals(hotness)
				&& user.activePosts.equals(activePosts);
	}

	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public int getSentPostsCount() {
		return sentPostsCount;
	}

	public void setSentPostsCount(int sentPosts) {
		this.sentPostsCount = sentPosts;
	}
}
