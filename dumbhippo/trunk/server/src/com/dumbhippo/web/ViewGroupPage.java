package com.dumbhippo.web;

import java.util.List;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class ViewGroupPage extends AbstractGroupPage {
	static private final int MAX_POSTS_SHOWN = 10;
	
	private PostingBoard postBoard;
	private Configuration configuration;
	
	public ViewGroupPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public List<PostView> getPosts() {
		assert getViewedGroup() != null;
		
		// we ask for 1 extra post to see if we need a "more posts" link
		return postBoard.getGroupPosts(signin.getViewpoint(), getViewedGroup(), 0, MAX_POSTS_SHOWN + 1);
	}
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
