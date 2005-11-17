/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web;


import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 * Displays a post in a frame with information about how it was shared
 * 
 * @author dff
 */

public class FramerPage {
    static private final Log logger = GlobalSetup.getLog(FramerPage.class);	
	
    private String postId;

    @Signin
		private SigninBean signin;
	
    private PostingBoard postBoard;
    private PostView post;
	private String chatroom;
	
    public FramerPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public String getPostId() {
		return postId;
    }

    public String getChatRoom() {
		String title = this.post.getTitle();
		String url = this.post.getUrl();
		if  ( title == url ) {
			// Get the domain name
			try {
				java.net.URL urlObject = new java.net.URL(url);
				title = urlObject.getHost();
			} catch (java.net.MalformedURLException e) {
				title = "Dumb Hippo Chat";
				logger.debug("Couldn't parse the URL object, created generic chat room: " + title);
			}
		}
		// Get rid of spaces for '+'
		title = title.replaceAll("[ \t\n\f\r]","+");
		// Remove any other weird characters
		title = title.replaceAll("[^a-zA-Z0-9\\+]","");
		if (title.length() >= 10)
			return title.substring(0, 10);
		else 
			return title;
    }

    protected void setPost(PostView post) {
		this.post = post;
		this.postId = post.getPost().getId();
		logger.debug("viewing post: " + this.postId);
    }

    public void setPostId(String postId) throws ParseException, GuidNotFoundException {
		if (postId == null) {
			logger.debug("no post id");
			return;
		} else {
			// Fixme - don't backtrace if the user isn't authorized to view the post
			setPost(postBoard.loadPost(signin.getViewpoint(), new Guid(postId)));
		}
    }
	
    public PostView getPost() {
		return post;
    }
}
