package com.dumbhippo.web;


import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostInfo;
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
	
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PostInfo postInfo;
	
	public FramerPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public String getPostId() {
		return postId;
	}
	
	protected void setPostInfo(PostInfo postInfo) {
		this.postInfo = postInfo;
		this.postId = postInfo.getPost().getId();
		logger.debug("viewing post: " + this.postId);
	}

	public void setPostId(String postId) throws ParseException, GuidNotFoundException {
		if (postId == null) {
			logger.debug("no post id");
			return;
		} else {
			setPostInfo(postBoard.loadPostInfo(new Guid(postId), signin.getUser()));
		}
	}
	
	public PostInfo getPostInfo() {
		return postInfo;
	}
}
