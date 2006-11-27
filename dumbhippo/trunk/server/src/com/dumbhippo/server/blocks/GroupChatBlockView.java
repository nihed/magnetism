package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.Viewpoint;

public class GroupChatBlockView extends BlockView implements SimpleTitleBlockView {
	public static final int RECENT_MESSAGE_COUNT = 3;
	
	private GroupView group;
	private List<ChatMessageView> recentMessages;
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(GroupView group, List<ChatMessageView> recentMessages) {
		this.group = group;
		this.recentMessages = recentMessages;
		setPopulated(true);
	}
	
	public GroupView getGroupView() {
		return this.group;
	}
	
	public List<ChatMessageView> getRecentMessages() {
		return recentMessages;
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("groupChat",
							"groupId", group.getIdentifyingGuid().toString());
		
		builder.openElement("recentMessages");
		for (ChatMessageView message : getRecentMessages()) {
			message.writeToXmlBuilder(builder);
		}
		builder.closeElement();
		
		builder.closeElement();
	}

	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(group);
		for (ChatMessageView message : getRecentMessages()) {
			result.add(message.getSenderView());
		}
		return result;
	}

	@Override
	public String getIcon() {
		// Mugshot stock favicon
		return "/images3/mugshot_icon.png";
	}

	public String getTitleForHome() {
		return getTitle();
	}
	
	public String getTitle() {
		return "New chat activity";
	}

	public String getLink() {
		// the chat link requires special handling - it's a javascript: link.
		return "";
	}

	@Override
	public String getTypeTitle() {
		return "Group Chat";
	}

	@Override
	protected String getSummaryHeading() {
		return getTitle();
	}

	@Override
	protected String getSummaryLink() {
		return group.getHomeUrl();
	}

	@Override
	protected String getSummaryLinkText() {
		return group.getName();
	}
}
