package com.dumbhippo.server;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class PostBlockView extends BlockView {
	private PostView postView;
	
	public PostBlockView(Block block, UserBlockData ubd, PostView post) {
		super(block, ubd);
		this.postView = post;
	}

	public String getWebTitleType() {
		return "Web Swarm";
	}
	
	public String getWebTitle() {
		return postView.getTitleAsHtml();
	}
	
	@Override
	public String getWebTitleLink() {
		return postView.getUrl();
	}
	
	public String getIconName() {
		return "webswarm_icon.png";
	}
	
	public PostView getPostView() {
		return this.postView;
	}
}
