package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.UserBlockData;

public class GroupChatBlockView extends BlockView {
	public static final int RECENT_MESSAGE_COUNT = 3;
	
	private GroupView group;
	private List<GroupMessage> recentMessages;
	
	public GroupChatBlockView(Block block, UserBlockData ubd, GroupView group, List<GroupMessage> recentMessages) {
		super(block, ubd);
		this.group = group;
		this.recentMessages = recentMessages;
	}

	public String getWebTitleType() {
		return "Mugshot";
	}
	
	public String getWebTitle() {
		return "Membership change";
	}
	
	public String getIconName() {
		return "mugshot_icon.png";
	}
	
	public GroupView getGroupView() {
		return this.group;
	}

	public List<GroupMessage> getRecentMessages(PostView postView) {
		return recentMessages;
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.openElement("group",
							"groupId", group.getIdentifyingGuid().toString());
		
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
		result.add(group);
		for (ChatMessage message : recentMessages) {
			result.add(message.getFromUser());
		}
		return result;
	}
}
