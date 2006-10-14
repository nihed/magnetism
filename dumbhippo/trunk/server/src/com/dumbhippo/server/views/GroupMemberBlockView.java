package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.UserBlockData;

public class GroupMemberBlockView extends BlockView {
	private GroupView group;
	private PersonView member;
	private MembershipStatus status;
	
	public GroupMemberBlockView(Block block, UserBlockData ubd, GroupView group, PersonView member, MembershipStatus status) {
		super(block, ubd);
		this.group = group;
		this.member = member;
		this.status = status;
	}

	@Override
	public String getWebTitleType() {
		return "Group update";
	}
	
	@Override
	public String getIconName() {
		return "mugshot_icon.png";
	}
	
	public GroupView getGroupView() {
		return this.group;
	}

	public PersonView getMemberView() {
		return this.member;
	}

	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("groupMember",
							    "groupId", group.getIdentifyingGuid().toString(),
							    "memberId", member.getIdentifyingGuid().toString(),
							    "status", status.name());
	}

	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(group);
		result.add(member);

		return result;
	}

	public MembershipStatus getStatus() {
		return status;
	}
}
