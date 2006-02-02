package com.dumbhippo.live;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.dumbhippo.identity20.Guid;

/**
 * The LivePost object represents current information about a
 * post. The set of information potentially exposed here includes 
 * both information that can be computed from the database, and 
 * information that is a function of the transient state of the
 * server. (Such as who is currently present in the post's chat
 * room.) 
 * 
 * @author otaylor
 */
public class LivePost implements Ageable {
	/**
	 * Get the "score" of a post. The score of a post is a
	 * measure of how active a post is. It is independent of
	 * the user viewing the post.
	 *  
	 * @return the score 
	 */
	public double getScore() {
		long now = System.currentTimeMillis();
		
		double score = 0.0;
		for (Viewer viewer : viewers) {
			if (viewer.viewedDate > now - 60 * 60 * 1000)
				score += 1.0;
		}
		
		return score;
	}
	
	/**
	 * Get the Post ID for which the LivePost object was created. 
	 * 
	 * @return the post ID for the post
	 */
	public Guid getPostId() {
		return postId;
	}
	
	/**********************************************************************/

	static final int MAX_STORED_VIEWERS = 5;

	private Guid postId;
	private int cacheAge;
	
	private static class Viewer {
		private Guid userId;
		private long viewedDate;
		
		public Viewer(Guid userId, Date viewedDate) {
			this.userId = userId;
			this.viewedDate = viewedDate.getTime();
		}
	}
	
	private List<Viewer> viewers;
	
	LivePost(Guid userId) {
		this.postId = userId;
		this.viewers = new LinkedList<Viewer>();
	}
	
	void addViewer(Guid userId, Date viewedDate) {
		int pos = 0;
		for (Viewer viewer : viewers) {
			if (viewer.userId.equals(userId))
				return; // already there
			if (viewer.viewedDate <= viewedDate.getTime())
				break;
			pos++;
		}
		
		if (pos == MAX_STORED_VIEWERS)
			return;
		
		if (viewers.size() == MAX_STORED_VIEWERS) 
			viewers.remove(viewers.size() -1);
		viewers.add(pos, new Viewer(userId, viewedDate));
	}

	/*************************************************************/

	public int getCacheAge() {
		return cacheAge;
	}

	public void setCacheAge(int cacheAge) {
		this.cacheAge = cacheAge;
	}
	
	public void discard() {
	}
}
