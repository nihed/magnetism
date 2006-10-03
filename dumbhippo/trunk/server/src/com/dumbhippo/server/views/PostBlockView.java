package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.UserBlockData;

public class PostBlockView extends BlockView {
	static final public int RECENT_MESSAGE_COUNT = 3;
	
	private PostView postView;
	private List<PostMessage> recentMessages;
	
	public PostBlockView(Block block, UserBlockData ubd, PostView post, List<PostMessage> recentMessages) {
		super(block, ubd);
		this.postView = post;
		this.recentMessages = recentMessages;
	}

	@Override
	public String getBlockId() {
		return postView.getPost().getId();
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
	
	public List<PostMessage> getRecentMessages(PostView postView) {
		return recentMessages;
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
		for (ChatMessage message : recentMessages) {
			builder.openElement("message",
								"serial", Long.toString(message.getId()),
								"timestamp",Long.toString(message.getTimestamp().getTime()),
								"sender", message.getFromUser().getId());
			builder.appendTextNode("text", message.getMessageText());
			builder.closeElement();
		}
		builder.closeElement();
		
		builder.closeElement();
	}

	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(postView);
		for (ChatMessage message : recentMessages) {
			result.add(message.getFromUser());
		}
		return result;
	}
}
