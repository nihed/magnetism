package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.Viewpoint;

public class GroupChatBlockView extends BlockView {
	public static final int RECENT_MESSAGE_COUNT = 3;
	
	private GroupView group;
	private List<ChatMessageView> recentMessages;
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, GroupView group, List<ChatMessageView> recentMessages) {
		super(viewpoint, block, ubd);
		this.group = group;
		this.recentMessages = recentMessages;
	}
	
	public GroupChatBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}
	
	public GroupView getGroupView() {
		return this.group;
	}

	public void setGroupView(GroupView group) {
		this.group = group;
	}
	
	public List<ChatMessageView> getRecentMessages() {
		return recentMessages;
	}
	
	public void setRecentMessages(List<ChatMessageView> recentMessages) {
		this.recentMessages = recentMessages;
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
}
