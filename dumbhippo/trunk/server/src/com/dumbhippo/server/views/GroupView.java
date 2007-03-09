package com.dumbhippo.server.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.LiveGroup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.VersionedEntity;

/**
 * @author otaylor
 *
 * This is a class encapsulating information about a Group that can be
 * returned out of the session tier and used by web pages; only the
 * constructor makes queries into the database; the read-only properties of 
 * this object access pre-computed data.
 */ 
 public class GroupView extends EntityView {
	 Group group;
	 GroupMember groupMember;
	 Set<PersonView> inviters;
	 int chattingUserCount;
	 
	 public GroupView(Group group, GroupMember groupMember, Set<PersonView> inviters) {
		 this.group = group;
		 this.groupMember = groupMember;
		 this.inviters = inviters;
	 }
	 
	 public Group getGroup() {
		 return group;
	 }
	 
	 public MembershipStatus getStatus() {
		 if (groupMember != null)
			 return groupMember.getStatus();
		 else
			 return MembershipStatus.NONMEMBER;
	 }
	 
	 public boolean isActive() {
		 return getStatus() == MembershipStatus.ACTIVE;
	 }
	 
	 public boolean isInvited() {
		 return getStatus() == MembershipStatus.INVITED;
	 }
	 
	 public boolean isInvitedToFollow() {
		 return getStatus() == MembershipStatus.INVITED_TO_FOLLOW;
	 }
	 
	 public boolean getCanJoin() {
		return !getGroupMember().getStatus().isParticipant() && 
		       (group.getAccess() == GroupAccess.PUBLIC_INVITE ||
		        getGroupMember().getStatus() == MembershipStatus.REMOVED);
	 }
	 
	 public boolean getCanSeeContent() {
         return (group.getAccess() != GroupAccess.SECRET || getGroupMember().getStatus().getCanSeeSecretContent());
	 }
	 
	 // TODO: avoid using this, and try using getInviters
	 public PersonView getInviter() {
		 
		 if (inviters == null) {
			 return null;
		 }
		 if (inviters.iterator().hasNext())
		     return inviters.iterator().next();
		 
		 return null;
	 }

	 public Set<PersonView> getInviters() {
		 return inviters;
	 }
	 
	/**
	 * Convert an (unordered) set of groups into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Group objects
	 * @return a newly created List containing the sorted groups
	 */
	 static public List<GroupView> sortedList(Set<GroupView> groups) {
		 ArrayList<GroupView> list = new ArrayList<GroupView>();
		 list.addAll(groups);
		 
		 final Collator collator = Collator.getInstance();
		 Collections.sort(list, new Comparator<GroupView>() {
			 public int compare (GroupView g1, GroupView g2) {
				 return collator.compare(g1.group.getName(), g2.group.getName());
			 }
		 });
		 
		 return list;
	 }

	@Override
	protected VersionedEntity getVersionedEntity() {
		return group;
	}
	
	@Override
	public void writeToXmlBuilderOld(XmlBuilder builder) {
		builder.appendTextNode("group", "", "id", group.getId(), "name", group.getName(),
							   "homeUrl", getHomeUrl(), "smallPhotoUrl", getPhotoUrl());		
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.appendTextNode("group", "", 
							   "id", group.getId(), 
							   "name", group.getName(),
							   "homeUrl", getHomeUrl(), 
							   "photoUrl", getPhotoUrl());		
	}

	@Override
	public String toIdentifyingXml() {
		XmlBuilder builder = new XmlBuilder();
		builder.appendTextNode("group", "", "id", group.getId());		
		return builder.toString();					
	}

	@Override
	public Guid getIdentifyingGuid() {
		return group.getGuid(); 
	}
	
	@Override
	public String getName() {
		return group.getName();
	}
	
	@Override
	public String getHomeUrl() {
		return "/group?who=" + group.getId();
	}
	
	@Override
	public String getPhotoUrl() {
		return group.getPhotoUrl();
	}
	
	public LiveGroup getLiveGroup() {
		return LiveState.getInstance().getLiveGroup(getGroup().getGuid());
	}

	public GroupMember getGroupMember() {
		return groupMember;
	}
	
	public int getChattingUserCount() {
		return PresenceService.getInstance().getPresentUsers("/rooms/" + group.getId(), 2).size();
	}
}
