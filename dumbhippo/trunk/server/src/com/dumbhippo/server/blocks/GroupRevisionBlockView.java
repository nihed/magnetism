package com.dumbhippo.server.blocks;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.GroupFeedAddedRevision;
import com.dumbhippo.persistence.GroupFeedRemovedRevision;
import com.dumbhippo.persistence.GroupNameChangedRevision;
import com.dumbhippo.persistence.GroupRevision;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;

public class GroupRevisionBlockView extends BlockView implements PersonSourceBlockView, TitleBlockView {
	private GroupView group;
	private PersonView revisor;
	private GroupRevision revision;
	
	public GroupRevisionBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) {
		super(viewpoint, block, ubd, participated);
	}

	public GroupRevisionBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) {
		super(viewpoint, block, gbd, participated);
	}

	void populate(GroupView group, PersonView revisor, GroupRevision revision) {
		this.group = group;
		this.revisor = revisor;
		this.revision = revision;
		setPopulated(true);
	}
	
	public GroupView getGroupView() {
		return this.group;
	}

	public PersonView getRevisorView() {
		return this.revisor;
	}

	public GroupRevision getRevision() {
		return this.revision;
	}
	
	@Override
	protected void writeDetailsToXmlBuilder(XmlBuilder builder) {
		builder.appendEmptyNode("groupMember",
							    "groupId", group.getIdentifyingGuid().toString(),
							    "revisorId", revisor.getIdentifyingGuid().toString());
	}

	public List<Object> getReferencedObjects() {
		List<Object> result = new ArrayList<Object>();
		result.add(group);
		result.add(revisor);

		return result;
	}
	
	@Override
	public String getPrivacyTip() {
		return "Private: This group change can only be seen by group members.";
	}	
	
	@Override
	public String getIcon() {
		// Mugshot stock favicon
		return "/images3/mugshot_icon.png";
	}

	@Override
	public String getTypeTitle() {
		// we don't display a type title for this kind of block, but if we did...
		return "Group changed";
	}
	
	@Override
	public String getSummaryHeading() {
		return "Group changed";
	}

	@Override
	public String getSummaryLink() {
		return group.getHomeUrl();
	}

	@Override
	public String getSummaryLinkText() {
		return group.getName();
	}

	public PersonView getEntitySource() {
		return getPersonSource();
	}

	public PersonView getPersonSource() {
		return revisor;
	}

	public String getTitleForHome() {
		switch (revision.getType()) {
		case GROUP_NAME_CHANGED:
			return "You changed a group's name to '" + ((GroupNameChangedRevision) revision).getNewName() + "'";
		case GROUP_DESCRIPTION_CHANGED: 
			return "You changed the group description for " + group.getName();
		case GROUP_FEED_ADDED:
			return "You added the feed '" + ((GroupFeedAddedRevision) revision).getFeed().getTitle() + "' to " + group.getName();
		case GROUP_FEED_REMOVED:
			return "You removed the feed '" + ((GroupFeedRemovedRevision) revision).getFeed().getTitle() + "' from " + group.getName();
		case USER_EXTERNAL_ACCOUNT_CHANGED:
		case USER_NAME_CHANGED:
		case USER_BIO_CHANGED:
			throw new RuntimeException("user revision using group revision block view");
		}
		throw new RuntimeException("unknown revision type in GroupRevisionBlockView.getTitleForHome()");
	}

	public String getTitle() {
		switch (revision.getType()) {
		case GROUP_NAME_CHANGED:
			return revisor.getName() + " changed the group's name to '" + ((GroupNameChangedRevision) revision).getNewName() + "'";		
		case GROUP_DESCRIPTION_CHANGED: 
			return revisor.getName() + " changed the group description for " + group.getName();
		case GROUP_FEED_ADDED:
			return revisor.getName() + " added the feed '" + ((GroupFeedAddedRevision) revision).getFeed().getTitle() + "' to " + group.getName();
		case GROUP_FEED_REMOVED:
			return revisor.getName() + " removed the feed '" + ((GroupFeedRemovedRevision) revision).getFeed().getTitle() + "' from " + group.getName();
		case USER_EXTERNAL_ACCOUNT_CHANGED:
		case USER_NAME_CHANGED:
		case USER_BIO_CHANGED:
			throw new RuntimeException("user revision using group revision block view");
		}
		throw new RuntimeException("unknown revision type in GroupRevisionBlockView.getTitle()");
	}

	public String getLink() {
		return "/group-account?group=" + revision.getTarget().getId();
	}
}
