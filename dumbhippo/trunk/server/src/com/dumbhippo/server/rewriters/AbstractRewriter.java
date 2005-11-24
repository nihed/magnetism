package com.dumbhippo.server.rewriters;

import java.net.URL;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostRewriter;

/**
 * Base class for rewriters provides basic "no-op" default 
 * implementations that don't change the underlying post.
 * 
 * @author hp
 */
public abstract class AbstractRewriter implements PostRewriter {

	static private final Log logger = GlobalSetup.getLog(AbstractRewriter.class);
	
	protected Post boundPost;
	protected URL boundUrl;
	private Runnable asyncTask;
	
	/**
	 * This should only be called once, in either the constructor
	 * or in bind()
	 * @param asyncTask the async task
	 */
	protected void setAsyncTask(Runnable asyncTask) {
		// not synchronized since getAsyncTask() hasn't been 
		// called yet
		this.asyncTask = asyncTask;
	}
	
	/**
	 * Subclass must call this from inside the async task,
	 * when the task is about to exit. They should call this in a
	 * finally block around all of run(), so exceptions
	 * don't cause a deadlock...
	 */
	protected void notifyAsyncTask() {
		synchronized (this) {
			logger.debug("notifying completion of async task for post " + boundPost.getId());
			asyncTask = null;
			notifyAll();
		}
	}
	
	/**
	 * All methods that rely on async task completion have 
	 * to do this at the beginning of the method
	 */
	protected void waitForAsyncTask() {
		synchronized (this) {
			logger.debug("waiting for async task for post " + boundPost.getId());
			while (asyncTask != null) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public void bind(Post post, URL url) {
		this.boundPost = post;
		this.boundUrl = url;
	}
	
	public Runnable getAsyncTask() {
		return asyncTask;
	}
	
	public String getTextAsHtml() {
		waitForAsyncTask();
		XmlBuilder builder = new XmlBuilder();
		builder.appendTextAsHtml(boundPost.getText());
		return builder.toString();
	}

	public String getTitle() {
		waitForAsyncTask();
		if (boundPost.getExplicitTitle() != null)
			return boundPost.getExplicitTitle();
		else if (boundPost.getResources() != null && !boundPost.getResources().isEmpty()) {
			// FIXME look for an url and use its title
		
			return "";
		} else {
			return "";
		}
	}
}
