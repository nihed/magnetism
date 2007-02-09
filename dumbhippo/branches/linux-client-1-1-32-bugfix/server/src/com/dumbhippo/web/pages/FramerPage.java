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
    private String errorText = "No post specified";

    @Signin
	private SigninBean signin;
	
    private PostingBoard postBoard;
    private PostView post;
    private boolean isVisit = false;
	
    public FramerPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
    }
	
    public SigninBean getSignin() {
		return signin;
    }

    public String getPostId() {
		return postId;
    }
    
    public String getErrorText() {
    	return errorText;
    }
    
    public void setIsVisit(boolean isVisit) {
    	this.isVisit = isVisit;
    }

     public void setPostId(String postId) {
		if (postId == null) {
			errorText = "No post specified";
			return;
		}

		try {
			Guid guid = new Guid(postId);
			this.postId = postId;
			
			this.post = postBoard.loadPost(signin.getViewpoint(), guid);
			logger.debug("viewing post: {}", this.postId);
			
			if (isVisit && signin.isValid()) {
				UserViewpoint viewpoint = (UserViewpoint)signin.getViewpoint();
				postBoard.postViewedBy(post.getPost(), viewpoint.getViewer());
			}
		} catch (NotFoundException e) {
			errorText = "Can't find a visible post for the post ID '" + postId + "'";
		} catch (ParseException e) {
			errorText = "The post ID '" + postId + "' doesn't make sense";
		}
    }
	
    public PostView getPost() {
		return post;
    }
}
