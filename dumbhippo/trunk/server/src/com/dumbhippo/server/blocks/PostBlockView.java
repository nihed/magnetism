package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.DateUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.Viewpoint;

public class PostBlockView extends BlockView implements TitleBlockView {
	static final public int RECENT_MESSAGE_COUNT = 3;
	
	private PostView postView;
	private List<ChatMessageView> recentMessages;
	
	public PostBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public PostBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	void populate(PostView post, List<ChatMessageView> recentMessages) {
		this.postView = post;
		this.recentMessages = recentMessages;
		
		setPopulated(true);
	}
	
	public PostView getPostView() {
		return this.postView;
	}
	
	public List<ChatMessageView> getRecentMessages() {
		return recentMessages;
	}
	
	public String getPostTimeAgo() {
		return DateUtils.formatTimeAgo(getPostView().getPost().getPostDate());
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
	
	@Override
	public String getIcon() {
		String feedIcon = postView.getFeedFavicon();
		if (feedIcon != null)
			return feedIcon;
		else
			return "/images3/webswarm_icon.png";
	}

	@Override
	public String getTypeTitle() {
		return "Web Swarm";
	}
	
	@Override
	public String getPrivacyTip() {
		return "Private: Only you and the recipients can see this.";
	}

	public String getTitleForHome() {
		return getTitle();
	}

	public String getTitle() {
		return postView.getTitle();
	}

	public String getLink() {
		return "/visit?post=" + postView.getPost().getId();
	}

	public @Override String getSummaryHeading() {
		return "Posted";
	}

	public @Override String getSummaryLink() {
		return getLink();
	}

	public @Override String getSummaryLinkText() {
		return getTitle();
	}
}
