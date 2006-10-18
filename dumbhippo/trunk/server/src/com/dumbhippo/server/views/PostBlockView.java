package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class PostBlockView extends BlockView {
	static final public int RECENT_MESSAGE_COUNT = 3;
	
	private PostView postView;
	private List<ChatMessageView> recentMessages;
	
	public PostBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, PostView post, List<ChatMessageView> recentMessages) {
		super(viewpoint, block, ubd);
		this.postView = post;
		this.recentMessages = recentMessages;
		setPopulated(true);
	}
	
	public PostBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	@Override
	public String getWebTitleType() {
		return "Web Swarm";
	}
	
	@Override
	public String getWebTitle() {
		return postView.getTitleAsHtml();
	}
	
	@Override
	public String getWebTitleLink() {
		return "/visit?post=" + postView.getIdentifyingGuid();
	}
	
	@Override
	public String getIconName() {
		return "webswarm_icon.png";
	}
	
	public PostView getPostView() {
		return this.postView;
	}
	
	public void setPostView(PostView postView) {
		this.postView = postView;
	}
	
	public List<ChatMessageView> getRecentMessages() {
		return recentMessages;
	}
	
	public void setRecentMessages(List<ChatMessageView> recentMessages) {
		this.recentMessages = recentMessages;
	}
	
	@Override
	public String getDescriptionHtml() {
		return postView.getTextAsHtml();
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("post",
							"postId", postView.getIdentifyingGuid().toString());
		
		builder.openElement("recentMessages");
		for (ChatMessageView message : getRecentMessages()) {
			message.writeToXmlBuilder(builder);
		}
		builder.closeElement();
		
		builder.closeElement();
	}

	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(postView);
		for (ChatMessageView message : getRecentMessages()) {
			result.add(message.getSenderView());
		}
		return result;
	}
}
