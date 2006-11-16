package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public class GroupMemberBlockView extends BlockView {
	private GroupView group;
	private PersonView member;
	private MembershipStatus status;
	private Set<PersonView> adders;
	
	public GroupMemberBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public GroupMemberBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	void populate(GroupView group, PersonView member, MembershipStatus status, Set<PersonView> adders) {
		this.group = group;
		this.member = member;
		this.status = status;
		this.adders = adders; // may be null
		setPopulated(true);
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
	
	public Set<PersonView> getAdders() {
		return adders;
	}
	
	@Override
	public String getIcon() {
		// Mugshot stock favicon
		return "/images3/mugshot_icon.png";
	}

	@Override
	public String getTypeTitle() {
		// we don't display a type title for this kind of block, but if we did...
		return "Group update";
	}
}
