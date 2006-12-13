package com.dumbhippo.server.updaters;

import java.net.URL;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.postinfo.PostInfo;

public interface PostUpdater {

	/**
	 * Sets the post and url this updater operates on.
	 * The updater does not modify the Post itself though, 
	 * it should treat the post as read-only.
	 *  
	 * @param post the post to update info on
	 * @param url the url to update info on
	 */
	public void bind(Post post, URL url);

	/**
	 * Whether it's necessary to call getUpdate() and get new PostInfo
	 * for this post. Call bind() before calling this. If this returns
	 * true then getUpdate() can't block.
	 * 
	 * @return true if no update is required
	 */
	public boolean isUpdated();
	
	/**
	 * Can block doing remote web services request. Should not 
	 * modify the Post in any way, just return the new 
	 * PostInfo. The caller is responsible for updating the 
	 * persistent PostInfo and timestamp in the Post.
	 * Call bind() before calling this. This call may 
	 * be invoked in a different thread from previous 
	 * method calls.
	 * 
	 * @return the new PostInfo to be set on the post
	 */
	public PostInfo getUpdate();
}
