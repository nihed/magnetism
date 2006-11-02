package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.UserBlockData;

public class GroupMemberBlockView extends BlockView {
	private GroupView group;
	private PersonView member;
	private MembershipStatus status;
	private Set<PersonView> adders;
	
	public GroupMemberBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, GroupView group, PersonView member, MembershipStatus status) {
		super(viewpoint, block, ubd);
		this.group = group;
		this.member = member;
		this.status = status;
	}

	public GroupMemberBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) {
		super(viewpoint, block, ubd);
	}

	public GroupView getGroupView() {
		return this.group;
	}
	
	public void setGroupView(GroupView group) {
		this.group = group;
	}

	public PersonView getMemberView() {
		return this.member;
	}
	
	public void setMemberView(PersonView member) {
		this.member = member;
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
	
	public void setStatus(MembershipStatus status) {
		this.status = status;
	}

	public void setAdders(Set<PersonView> adders) {
		this.adders = adders;
	}
	
	public Set<PersonView> getAdders() {
		return adders;
	}
	
	@Override
	public String getIcon() {
		// Mugshot stock favicon
		return "/images3/mugshot_icon.png";
	}	
}
