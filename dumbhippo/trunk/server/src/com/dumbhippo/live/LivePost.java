package com.dumbhippo.live;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.dumbhippo.XmlBuilder;
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
public class LivePost extends LiveObject {
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
	/**********************************************************************/

	static final int MAX_STORED_VIEWERS = 5;

	private int recentMessageCount;
	private int chattingUserCount;
	private int viewingUserCount;
	private int totalViewerCount;
	
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
		super(userId);
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
	
	public Set<Guid> getViewers() {
		Set<Guid> viewerGuids = new HashSet<Guid>();
		for (Viewer v : this.viewers) {
			viewerGuids.add(v.userId);
		}
		return viewerGuids;
	}

	/*************************************************************/

	public void discard() {
	}

	public int getRecentMessageCount() {
		return recentMessageCount;
	}

	public void setRecentMessageCount(int recentMessageCount) {
		this.recentMessageCount = recentMessageCount;
	}

	public int getChattingUserCount() {
		return chattingUserCount;
	}

	public void setChattingUserCount(int chattingUserCount) {
		this.chattingUserCount = chattingUserCount;
	}
	
	public int getViewingUserCount() {
		return viewingUserCount;
	}

	public void setViewingUserCount(int chattingUserCount) {
		this.viewingUserCount = chattingUserCount;
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
		if (!(arg instanceof LivePost))
			return false;
		LivePost post = (LivePost) arg;
		return super.equals(post)
				&& post.recentMessageCount == recentMessageCount
				&& post.chattingUserCount == chattingUserCount
				&& post.viewingUserCount == viewingUserCount
				&& post.totalViewerCount == totalViewerCount;
	}
	
	public int getTotalViewerCount() {
		return totalViewerCount;
	}

	public void setTotalViewerCount(int totalViewerCount) {
		this.totalViewerCount = totalViewerCount;
	}	
	
	public String toXml() {
		XmlBuilder builder = new XmlBuilder();
		builder.openElement("livepost", "id", getGuid().toString());
		builder.openElement("recentViewers");
		for (Viewer viewer : viewers) {
			builder.appendTextNode("viewer", "", "id", viewer.userId.toString());
		}
		builder.closeElement(); // recentViewers
		builder.appendTextNode("chattingUserCount", ""+getChattingUserCount());
		builder.appendTextNode("viewingUserCount", ""+getViewingUserCount());
		builder.appendTextNode("totalViewers", ""+getTotalViewerCount());
		builder.closeElement(); // livepost
		return builder.toString();
	}
}
