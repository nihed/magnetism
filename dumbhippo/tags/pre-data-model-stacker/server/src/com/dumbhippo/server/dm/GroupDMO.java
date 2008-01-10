package com.dumbhippo.server.dm;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMInit;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@DMO(classId="http://mugshot.org/p/o/group", resourceBase="/o/group")
@DMFilter("viewer.canSeeGroup(this)")
public abstract class GroupDMO extends DMObject<Guid> {
	@Inject
	private EntityManager em;
	
	@Inject
	private DMSession session;
	
	@Inject
	private Viewpoint viewpoint;

	private Group group;
	
	Set<UserDMO> invitedToFollowMembers;
	Set<UserDMO> followerMembers;
	Set<UserDMO> invitedMembers;
	Set<UserDMO> activeMembers;
	Set<UserDMO> removedMembers;
	
	Set<UserDMO> canSeeMembers;
	
	protected GroupDMO(Guid key) {
		super(key);
	}
	
	static private final int MEMBERS_GROUP = 1;

	@Override
	protected void init() throws NotFoundException {
		group = em.find(Group.class, getKey().toString());
		if (group == null)
			throw new NotFoundException("No such group");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return group.getName();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getPhotoUrl() {
		return group.getPhotoUrl();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getHomeUrl() {
		return "/group?who=" + group.getId();
	}
	
	@DMProperty
	public boolean isPublic() {
		return group.isPublic();
	}
	
	private boolean viewerIsInSet(Guid viewerId, String propertyName) {
		try {
			@SuppressWarnings("unchecked")
			Set<Guid> guids = (Set<Guid>)session.getRawProperty(GroupDMO.class, getKey(), propertyName);
			return guids.contains(viewerId);
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	@DMProperty(cached=false, defaultInclude=true)
	public String getStatus() {
		MembershipStatus status = MembershipStatus.NONMEMBER;
		
		if (viewpoint instanceof UserViewpoint) {
			Guid viewerId = ((UserViewpoint)viewpoint).getViewerId();
			
			if (viewerIsInSet(viewerId, "activeMembers"))
				status = MembershipStatus.ACTIVE;
			else if (viewerIsInSet(viewerId, "followerMembers"))
				status = MembershipStatus.FOLLOWER;
			else if (viewerIsInSet(viewerId, "invitedMembers"))
				status = MembershipStatus.INVITED;
			else if (viewerIsInSet(viewerId, "invitedToFollowMembers"))
				status = MembershipStatus.INVITED_TO_FOLLOW;
			else if (viewerIsInSet(viewerId, "removedMembers"))
				status = MembershipStatus.REMOVED;
		}
		
		return status.name();
	}
	
	@DMInit(group=MEMBERS_GROUP)
	public void initMembers() {
		invitedToFollowMembers = new HashSet<UserDMO>();
		followerMembers = new HashSet<UserDMO>();
		removedMembers = new HashSet<UserDMO>();
		invitedMembers = new HashSet<UserDMO>();
		activeMembers = new HashSet<UserDMO>();
		canSeeMembers = new HashSet<UserDMO>();

		for (GroupMember gm : group.getMembers()) {
			AccountClaim accountClaim = gm.getMember().getAccountClaim();
			if (accountClaim != null) {
				UserDMO user = session.findUnchecked(UserDMO.class, accountClaim.getOwner().getGuid());
				
				switch (gm.getStatus()) {
				case NONMEMBER:
					break;
				case INVITED_TO_FOLLOW:
					invitedToFollowMembers.add(user);
					break;
				case FOLLOWER:
					followerMembers.add(user);
					break;
				case REMOVED:
					removedMembers.add(user);
					break;
				case INVITED:
					invitedMembers.add(user);
					break;
				case ACTIVE:
					activeMembers.add(user);
					break;
				}
			
				if (gm.getStatus().getCanSeeSecretGroup())
					canSeeMembers.add(session.findUnchecked(UserDMO.class, accountClaim.getOwner().getGuid()));
			}
		}
	}

	// These are used to compute the status
	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getInvitedToFollowMembers() {
		return invitedToFollowMembers;
	}
	
	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getFollowerMembers() {
		return followerMembers;
	}

	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getRemovedMembers() {
		return removedMembers;
	}

	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getInvitedMembers() {
		return invitedMembers;
	}

	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getActiveMembers() {
		return activeMembers;
	}

	// for visibility purposes; this will include members that have removed themselves, so
	// it shouldn't be used for display purposes.
	@DMProperty(group=MEMBERS_GROUP)
	public Set<UserDMO> getCanSeeMembers() {
		return canSeeMembers;
	}
}
