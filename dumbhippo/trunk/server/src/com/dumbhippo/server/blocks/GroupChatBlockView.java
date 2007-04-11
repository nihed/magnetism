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

public class GroupChatBlockView extends BlockView implements TitleBlockView {
	private GroupView group;
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}
	
	void populate(GroupView group, List<ChatMessageView> recentMessages, int messageCount) {
		this.group = group;
		setRecentMessages(recentMessages);
		setMessageCount(messageCount);
		setPopulated(true);
	}
	
	public GroupView getGroupView() {
		return this.group;
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
	
	@Override
	public String getPrivacyTip() {
		return "Private: This group chat can only be seen by group members.";
	}

	public String getLink() {
		// the chat link requires special handling - it's a javascript: link.
		return "";
	}

	@Override
	public String getTypeTitle() {
		return "Group Chat";
	}

	public @Override String getSummaryHeading() {
		return getTitle();
	}

	public @Override String getSummaryLink() {
		return group.getHomeUrl();
	}

	public @Override String getSummaryLinkText() {
		return group.getName();
	}
	
	@Override
	public String getChatId() {
		return group.getGroup().getId();
	}
	
	@Override
	public String getChatKind() {
		return "group";
	}
}
