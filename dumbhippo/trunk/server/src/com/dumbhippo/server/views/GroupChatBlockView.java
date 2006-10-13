package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;

public class GroupChatBlockView extends BlockView {
	public static final int RECENT_MESSAGE_COUNT = 3;
	
	private GroupView group;
	private List<ChatMessageView> recentMessages;
	
	public GroupChatBlockView(Block block, UserBlockData ubd, GroupView group, List<ChatMessageView> recentMessages) {
		super(block, ubd);
		this.group = group;
		this.recentMessages = recentMessages;
	}

	@Override
	public String getWebTitleType() {
		return "Mugshot";
	}
	
	@Override
	public String getWebTitle() {
		return "Membership change";
	}
	
	@Override
	public String getIconName() {
		return "mugshot_icon.png";
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
}
