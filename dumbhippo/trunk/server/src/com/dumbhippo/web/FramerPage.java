/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web;


import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * Displays a post in a frame with information about how it was shared
 * 
 * @author dff
 */

public class FramerPage {
    static private final Logger logger = GlobalSetup.getLogger(FramerPage.class);	
	
    private String postId;

    @Signin
		private SigninBean signin;
	
    private PostingBoard postBoard;
    private PostView post;
	
    public FramerPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public String getPostId() {
		return postId;
    }

    protected void setPost(PostView post) {
		this.post = post;
		this.postId = post.getPost().getId();
		logger.debug("viewing post: " + this.postId);
		if (signin.isValid()) {
			postBoard.postViewedBy(this.postId, signin.getUser());
		}
    }

    public void setPostId(String postId) throws ParseException, NotFoundException {
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
