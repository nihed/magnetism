/* -*- tab-width: 4; indent-tabs-mode: t -*- */
package com.dumbhippo.web.pages;


import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

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
		logger.debug("viewing post: {}", this.postId);
		if (signin.isValid()) {
			UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
			postBoard.postViewedBy(this.postId, viewpoint.getViewer());
		}
    }

    public void setPostId(String postId) throws ParseException, NotFoundException {
		if (postId == null) {
			logger.debug("no post id");
			return;
		} else {
			// FIXME - don't backtrace if the user isn't authorized to view the post
			// (most pages handle this with dht:errorPage, find an example and copy that)
			setPost(postBoard.loadPost(signin.getViewpoint(), new Guid(postId)));
		}
    }
	
    public PostView getPost() {
		return post;
    }
}
