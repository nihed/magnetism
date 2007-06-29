package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Post;

@Local
public interface PostInfoSystem {

	/**
	 * Let the system know we'll update the post info shortly.
	 * This starts up a thread to do any time-consuming work
	 * that needs doing to update the post.
	 * @param post the post we'll want to update soon
	 */
	public void hintWillUpdateSoon(Post post);
	
	/**
	 * Update the post info fields in the Post object. 
	 * The post should be attached so the updated information 
	 * gets persisted.
	 * 
	 * @param post the post to update
	 */
	public void updatePostInfo(Post post);
}
