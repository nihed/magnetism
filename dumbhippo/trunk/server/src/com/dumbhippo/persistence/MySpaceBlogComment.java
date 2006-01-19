package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class MySpaceBlogComment extends DBUnique {
	private static final long serialVersionUID = 1L;

	MySpaceResource blog;
	long commentId;
	long posterId;
	long discoveredDate;
	
	protected MySpaceBlogComment() {}
	
	public MySpaceBlogComment(MySpaceResource blog, long commentId, long posterId) {
		this.blog = blog;
		this.commentId = commentId;
		this.posterId = posterId;
		this.discoveredDate = new Date().getTime();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)	
	public MySpaceResource getBlog() {
		return blog;
	}
	public void setBlog(MySpaceResource blog) {
		this.blog = blog;
	}
	
	@Column(nullable=false)	
	public long getCommentId() {
		return commentId;
	}
	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}
	
	@Column(nullable=false)	
	public long getDiscoveredDate() {
		return discoveredDate;
	}
	public void setDiscoveredDate(long discoveredDate) {
		this.discoveredDate = discoveredDate;
	}
	
	@Column(nullable=false)	
	public long getPosterId() {
		return posterId;
	}
	public void setPosterId(long posterId) {
		this.posterId = posterId;
	}
}
