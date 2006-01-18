package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;

@Entity
public class MySpaceBlogComment extends DBUnique {
	private static final long serialVersionUID = 1L;

	MySpaceResource blog;
	long commentId;
	long posterId;
	long discoveredDate;
	boolean seen;
	
	public MySpaceBlogComment(MySpaceResource blog, long commentId, long posterId) {
		this.blog = blog;
		this.commentId = commentId;
		this.posterId = posterId;
		this.discoveredDate = new Date().getTime();
		this.seen = false;
	}
	
	public MySpaceResource getBlog() {
		return blog;
	}
	public void setBlog(MySpaceResource blog) {
		this.blog = blog;
	}
	public long getCommentId() {
		return commentId;
	}
	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}
	public long getDiscoveredDate() {
		return discoveredDate;
	}
	public void setDiscoveredDate(long discoveredDate) {
		this.discoveredDate = discoveredDate;
	}
	public long getPosterId() {
		return posterId;
	}
	public void setPosterId(long posterId) {
		this.posterId = posterId;
	}
	public boolean isSeen() {
		return seen;
	}
	public void setSeen(boolean seen) {
		this.seen = seen;
	}
}
