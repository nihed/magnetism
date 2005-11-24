package com.dumbhippo.persistence;

import java.net.URL;


public interface PostRewriter {

	/**
	 * Binds the rewriter to a post; this post 
	 * will be the post we rewrite. Must be 
	 * called only once, prior to any other methods.
	 * @param post the post
	 * @param url the url which matched the rewriter
	 */
	public void bind(Post post, URL url);
	
	/**
	 * The PostRewriter may want to do some asynchronous work
	 * to get the rewrite information. This method must be 
	 * called after bind() and before anything else; if it 
	 * returns non-null, all other methods will block until 
	 * the Runnable has been executed and completed.
	 * 
	 * @return an async task
	 */
	public Runnable getAsyncTask();
	
	public String getTextAsHtml();
	
	public String getTitle();
	
}
