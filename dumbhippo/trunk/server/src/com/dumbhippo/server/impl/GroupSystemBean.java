package com.dumbhippo.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Hits;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.GroupEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.Validators;
import com.dumbhippo.search.SearchSystem;
import com.dumbhippo.server.GroupIndexer;
import com.dumbhippo.server.GroupSearchResult;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.GroupMemberView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(GroupSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private SearchSystem searchSystem;
	
	@EJB
	private Notifier notifier;
	
	public Group createGroup(User creator, String name, GroupAccess access, String description) {
		if (creator == null)
			throw new IllegalArgumentException("null group creator");
		
		Group g = new Group(name, access);
		g.setDescription(description);
		
		em.persist(g);
		
		GroupMember groupMember = new GroupMember(g, creator.getAccount(), MembershipStatus.ACTIVE);
		
		em.persist(groupMember);
		
		// Fix up the inverse side of the mapping
		g.getMembers().add(groupMember);
		
		notifier.onGroupCreated(g);
		notifier.onGroupMemberCreated(groupMember, System.currentTimeMillis());
		
		searchSystem.indexGroup(g, false);
		
		return g;
	}

	private GroupMember getGroupMemberForUser(Group group, User user, boolean fixupExpected) throws NotFoundException {
		List<GroupMember> allMembers = new ArrayList<GroupMember>(); 
		for (GroupMember member : group.getMembers()) {
			AccountClaim ac = member.getMember().getAccountClaim();
			if (ac != null && ac.getOwner().equals(user)) {
				allMembers.add(member);
			}
		}
		
		if (allMembers.isEmpty())
			throw new NotFoundException("GroupMember for user " + user + " in group " + group + " not found");
		
		// if !fixupExpected we could always just return here (assuming allMembers.size() == 1) but for now
		// run through the below sanity checking and throw RuntimeException if stuff is wonky
		
		/* We fix up here to always have a single member which is the account member */
		
		MembershipStatus status = MembershipStatus.NONMEMBER;
		Set<User> adders = null;
		GroupMember accountMember = null;
		Account account = user.getAccount();
		for (GroupMember member : allMembers) {
			// use our "most joined up" status
			if (member.getStatus().ordinal() > status.ordinal()) {
				status = member.getStatus();
				// prefer the "most joined" adders, but prefer any 
				// adders over no adders
				if (!member.getAdders().isEmpty())
					adders = member.getAdders();
			}
			
			if (member.getMember().equals(account)) {
				accountMember = member;
			} else {
				if (fixupExpected) {
					// get rid of anything that isn't an account member
					logger.debug("Removing group member {} in favor of account member", member);
					// update the flag on account in case new invitations are awaiting
					// this is useful when a person signs in for the first time and gets an account,
				    // we want them to see that they have new group invitations
					if (member.getStatus() == MembershipStatus.INVITED) {
						account.touchGroupInvitationReceived();
					}
					group.getMembers().remove(member);					
					em.remove(member);
				} else {
					logger.debug("fixup would remove {}", member);
					throw new RuntimeException("Unexpected need to fixup GroupMember for user " + user + " in group " + group);
				}
			}
		}
		
		if (status == MembershipStatus.NONMEMBER) {
			throw new RuntimeException("MembershipStatus.NONMEMBER found in the database for group " + group + " user " + user);
		}
		
		if (accountMember == null) {
			if (!fixupExpected) {
				throw new RuntimeException("Fixups not expected and group member was not an account member: " + allMembers);
			}
			logger.debug("Adding new account member to group {} for account {}", group, account);
			accountMember = new GroupMember(group, account, status);
			if (adders != null && !adders.isEmpty())
				accountMember.setAdders(adders);
			em.persist(accountMember);
			group.getMembers().add(accountMember);
			notifier.onGroupMemberCreated(accountMember, System.currentTimeMillis());
		}

		return accountMember;
	}

	private GroupMember getGroupMemberForContact(Group group, Contact contact) throws NotFoundException {		
		// FIXME this isn't really going to work well if there are multiple GroupMember, but the 
		// UI doesn't really offer a way to do that right now anyway I don't think
		
		// This would work fine by itself without the above code, if there
		// were never anything that needed fixup
		for (GroupMember member : group.getMembers()) {
			for (ContactClaim cc : contact.getResources()) {
				if (cc.getResource().equals(member.getMember()))
					return member;
				// Because we don't fixup ContactClaim to point to the Account instead of the
				// EmailResource when someone you invited joins, we need an additional
				// check here to see if the EmailResource is claimed by a user who is
				// a group member.
				if (!(cc.getResource() instanceof Account)) {				
				    AccountClaim ac = cc.getResource().getAccountClaim();
				    if (ac != null && ac.getOwner().getAccount().equals(member.getMember())) {
					    return member;
				    }
				}
			}
		}
		throw new NotFoundException("GroupMember for contact " + contact + " not found");
	}
	
	private GroupMember getGroupMember(Group group, Person person) throws NotFoundException {
		if (person instanceof User)
			return getGroupMemberForUser(group, (User)person, true);
		else
			return getGroupMemberForContact(group, (Contact)person);
	}
	
	public GroupMember getGroupMember(Group group, Resource resource) throws NotFoundException {
		for (GroupMember member : group.getMembers()) {
			if (member.getMember().equals(resource))
				return member;
		}
		throw new NotFoundException("GroupMember for resource " + resource + " not found");
	}

	public GroupMember getGroupMember(Group group, Guid resourceId) throws NotFoundException {
		for (GroupMember member : group.getMembers()) {
			if (member.getMember().getGuid().equals(resourceId))
				return member;
		}
		throw new NotFoundException("GroupMember for resource " + resourceId + " not found");
	}
	
	public Set<Group> getInvitedToGroups(User adder, Resource invitee) {
		Set<Group> invitedToGroups = new HashSet<Group>();
		
		Set<Group> adderGroups = 
			findRawGroups(new UserViewpoint(adder), adder, MembershipStatus.ACTIVE);

	    for (Group group : adderGroups) {
	        try {
	        	GroupMember member = getGroupMember(group, invitee);
	        	if (member.getAdders().contains(adder)) {
	        		invitedToGroups.add(group);
	        	}
	        } catch (NotFoundException e) {
	        	// adder is not a member of this group, nothing to do
	        }
	    }
	    
	    return invitedToGroups;
	}
	
	public boolean canSeeContent(Viewpoint viewpoint, Group group) {
		if (group.getAccess() != GroupAccess.SECRET || viewpoint instanceof SystemViewpoint) {
			return true;
		} else if (viewpoint instanceof UserViewpoint) {
			return getViewerStatus(viewpoint, group).getCanSeeSecretContent();
		} else {
			return false;
		}
	}
	
	public boolean canAddMembers(User adder, Group group) {
		GroupMember adderMember;
		try {
			adderMember = getGroupMember(group, adder);
			return adderMember.getStatus().getCanAddMembers();
		} catch (NotFoundException e) {
			return false;
		}		
	}
	
	public void addMember(User adder, Group group, Person person) {
		GroupMember groupMember;
		
		boolean selfAdd = adder.equals(person);
		boolean canAddSelf = false;
		
		try {
			// if person is a User then this will do fixups
			groupMember = getGroupMember(group, person);
			canAddSelf = (groupMember.getStatus().ordinal() >= MembershipStatus.REMOVED.ordinal() 
					      || group.getAccess() == GroupAccess.PUBLIC);
		} catch (NotFoundException e) {
			groupMember = null;
			canAddSelf = (group.getAccess() == GroupAccess.PUBLIC);
		}
		
		boolean adderCanAdd = canAddMembers(adder, group);
		
		long now = System.currentTimeMillis();
		
		MembershipStatus oldStatus;
		if (groupMember != null)
			oldStatus = groupMember.getStatus();
		else
			oldStatus = MembershipStatus.NONMEMBER;

		// Compute the best status that the adder can make the user
		MembershipStatus newStatus;
		if (selfAdd)
			newStatus = canAddSelf ? MembershipStatus.ACTIVE : MembershipStatus.FOLLOWER;
		else
			newStatus = adderCanAdd ? MembershipStatus.INVITED : MembershipStatus.INVITED_TO_FOLLOW;
		
		// If that's worse than the current status, then use the current status
		if (newStatus.worseThan(oldStatus))
			newStatus = oldStatus;

		if (groupMember != null) {
			switch (groupMember.getStatus()) {
			case NONMEMBER:
				throw new IllegalStateException();
			case FOLLOWER:
				// Followers always transition directly to ACTIVE, we don't
				// want a 3 way handshake.
				if (newStatus == MembershipStatus.INVITED) 
					newStatus = MembershipStatus.ACTIVE;
				break;
			case INVITED_TO_FOLLOW:
				if (!selfAdd)
					groupMember.addAdder(adder);				
				break;
			case REMOVED:
				if (!selfAdd) {
					groupMember.addAdder(adder); // Mark adder for "please come back"
					// We don't want to change REMOVED status except by the user
					// who was removed
					return;
				}
				break;
			case INVITED:
				if (!selfAdd) {
					// already invited, add the new adder, let the block be restacked  
					groupMember.addAdder(adder);	
				}
				break;
			case ACTIVE:
				return; // Nothing to do
			}
			groupMember.setStatus(newStatus);
			notifier.onGroupMemberStatusChanged(groupMember, now);
			
		} else {
			Resource resource = identitySpider.getBestResource(person);
		    
			groupMember = new GroupMember(group, resource, newStatus);
			if (!selfAdd) { 
				groupMember.addAdder(adder);
				// Adding to a group for the first time, touch their last 
				// added date
				if (resource instanceof Account) {
					Account acct = (Account) resource;
					acct.touchGroupInvitationReceived();
				}
			}
			em.persist(groupMember);
			group.getMembers().add(groupMember);
			em.persist(group);
			
			notifier.onGroupMemberCreated(groupMember, now);
		}
		
        LiveState.getInstance().queueUpdate(new GroupEvent(group.getGuid(), groupMember.getMember().getGuid(),
        		GroupEvent.Detail.MEMBERS_CHANGED));
	}
	
	public boolean canRemoveInvitation(User remover, GroupMember groupMember) {
		if (groupMember.getAdders().contains(remover) && !(groupMember.getMember() instanceof Account)) {
			return true;
		}
		return false;
	}
	
	public void removeMember(User remover, Group group, Person person) {	
		try {
			// note that getGroupMember() here does a fixup so we only have one GroupMember which 
			// canonically points to our account.
			GroupMember groupMember = getGroupMember(group, person);
			removeMember(remover, groupMember);
		} catch (NotFoundException e) {
			// nothing to do
		}
	}
	
	public void removeMember(User remover, GroupMember groupMember) {		
	   User userMember = identitySpider.getUser(groupMember.getMember());
	   Group group = groupMember.getGroup();
	   
        // we let adders remove group members they added that do not have an account
		if (!remover.equals(userMember) && canRemoveInvitation(remover, groupMember)) {
			groupMember.removeAdder(remover);
			// entirely remove the group member if the last adder removed them
			if (groupMember.getAdders().isEmpty()) {
				group.getMembers().remove(groupMember);
				em.remove(groupMember);					
			}
		} else if (!remover.equals(userMember)) {				
			throw new IllegalArgumentException("a group member can only be removed by themself " +
					                           "or by one of its adders if the group member doesn't have an account");
		}
		
		// REMOVED has more rights than FOLLOWER so be sure we don't let followers "remove" themselves. 
		if (groupMember.getStatus().ordinal() > MembershipStatus.REMOVED.ordinal()) {
			groupMember.setStatus(MembershipStatus.REMOVED);
			
			notifier.onGroupMemberStatusChanged(groupMember, System.currentTimeMillis());
	        LiveState.getInstance().queueUpdate(new GroupEvent(group.getGuid(),
	        		groupMember.getMember().getGuid(), GroupEvent.Detail.MEMBERS_CHANGED));
		} else if (groupMember.getStatus().ordinal() < MembershipStatus.REMOVED.ordinal()) {
			// To go from FOLLOWER or INVITED_TO_FOLLOW to removed, we delete the GroupMember
			group.getMembers().remove(groupMember);
			em.remove(groupMember);
			
			// we don't stackGroupMember here, we only care about transitions to REMOVED for timestamp 
			// updating (right now anyway)
			LiveState.getInstance().queueUpdate(new GroupEvent(group.getGuid(),
					groupMember.getMember().getGuid(), GroupEvent.Detail.MEMBERS_CHANGED));
		} else {
			// status == REMOVED, nothing to do
		}
	}
	
	// this checks if the viewer can see the group activity or its members
	private static final String CAN_SEE = 
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
		  "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
	              "WHERE vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " +
	              "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ")) ";

	private static final String CAN_SEE_ANONYMOUS = " g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " ";
	
	private String getStatusClause(MembershipStatus status) {
		return getStatusClause(status, false);
	}

	private String getStatusClause(MembershipStatus status, boolean allGroups) {
		if (status != null) {
			return " AND gm.status = " + status.ordinal();
		} else if (allGroups) {
			return " AND ( gm.status >= " + MembershipStatus.INVITED.ordinal() +
			       " OR gm.status = " + MembershipStatus.INVITED_TO_FOLLOW.ordinal() +
			       " OR gm.status = " + MembershipStatus.FOLLOWER.ordinal() + " )";
		} else {
			return " AND gm.status >= " + MembershipStatus.INVITED.ordinal();
		}
	}
	
	private String getReceivesClause() {
		Set<String> receivesOrdinals = new HashSet<String>();
		for (MembershipStatus status : MembershipStatus.values()) {
			if (status.getReceivesPosts()) {
				receivesOrdinals.add(Integer.toString(status.ordinal()));
			}
		}
		return " AND gm.status in (" + StringUtils.join(receivesOrdinals, ", ") + ") ";
	}
	
	private Query buildGetResourceMembersQuery(Viewpoint viewpoint, Group group, MembershipStatus status, boolean isCount) {
		StringBuilder queryString = new StringBuilder("SELECT ");
		Query q;
		
		if (isCount)
			queryString.append("count(gm.member)");
		else
			queryString.append("gm.member");
		
		queryString.append(" FROM GroupMember gm, Group g " +
				"WHERE gm.group = :group AND g = :group");
		
		String statusClause = getStatusClause(status);
		if (viewpoint instanceof SystemViewpoint) {
			q = em.createQuery(queryString + statusClause);
		} else if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint)viewpoint).getViewer();
			q = em.createQuery(queryString + " AND " + CAN_SEE + statusClause)
		    	.setParameter("viewer", viewer);
		} else {
			q = em.createQuery(queryString + " AND " + CAN_SEE_ANONYMOUS + statusClause);
		}
		q.setParameter("group", group);
		return q;
	}
		
	private List<Resource> getResourceMembers(Viewpoint viewpoint, Group group, int maxResults, MembershipStatus status) {
		Query q = buildGetResourceMembersQuery(viewpoint, group, status, false);
		if (maxResults >= 0)
			q.setMaxResults(maxResults);
		@SuppressWarnings("unchecked")
		List<Resource> result = q.getResultList();
		return result;
	}

	public int getMembersCount(Viewpoint viewpoint, Group group, MembershipStatus status) {
		Query q = buildGetResourceMembersQuery(viewpoint, group, status, true);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();		
	}
	
	public Set<GroupMemberView> getMembers(Viewpoint viewpoint, Group group, PersonViewExtra... extras) {
		return getMembers(viewpoint, group, null, -1, extras);
	}
	
	public Set<GroupMemberView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, int maxResults, PersonViewExtra... extras) {
		
		// The subversion history has some code to try doing this with fewer queries; 
		// but for now keeping it simple
		
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, maxResults, status);
		if (resourceMembers.size() == 0)
			return Collections.emptySet();
		
		Set<GroupMemberView> result = new HashSet<GroupMemberView>();
		logger.debug("will generate person views for {} resources", resourceMembers.size());
		for (Resource r : resourceMembers) {
			GroupMemberView groupMemberView = personViewer.getPersonView(viewpoint, r, GroupMemberView.class, extras); 
			if (viewpoint instanceof UserViewpoint) {
				try {
			        groupMemberView.setViewerCanRemoveInvitation(canRemoveInvitation(((UserViewpoint)viewpoint).getViewer(), getGroupMember(group, r)));
				} catch (NotFoundException e) {
					throw new RuntimeException("Couldn't find a group member that was returned by getResourceMembers", e);
				}
			}
			result.add(groupMemberView); 
		}
		
		return result;
	}
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group) {
		return getUserMembers(viewpoint, group, null);
	}
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, -1, status);
		
		Set<User> result = new HashSet<User>();
		for (Resource r : resourceMembers) {
			User user = identitySpider.getUser(r);
			if (user != null)
				result.add(user);
		}
		
		return result;
	}
	
	// get people who should be notified on new followers/members
	public Set<User> getMembershipChangeRecipients(Group group) {
		return getUserMembers(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE);
	}
	
	private MembershipStatus getViewerStatus(Viewpoint viewpoint, Group group) {
		if (viewpoint instanceof SystemViewpoint) {
			// System viewpoint as the same visibility as a member
			return MembershipStatus.ACTIVE;
		} else if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint)viewpoint).getViewer();
			try {
				GroupMember viewerGroupMember = getGroupMember(group, viewer);
				return viewerGroupMember.getStatus();
			} catch (NotFoundException e) {
				return MembershipStatus.NONMEMBER;
			}
		} else {			
			return MembershipStatus.NONMEMBER;
		}
	}
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member) throws NotFoundException {
		if (!viewpoint.isOfUser(member) &&
			group.getAccess() == GroupAccess.SECRET &&
			!getViewerStatus(viewpoint, group).getCanSeeSecretContent())
			throw new NotFoundException("GroupMember for user " + member + " not found");
			
		return getGroupMember(group, member);
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	private static final String FIND_RAW_GROUPS_QUERY = 
		"SELECT gm.group FROM GroupMember gm, AccountClaim ac, Group g " +
		"WHERE ac.resource = gm.member AND ac.owner = :member AND g = gm.group ";
	
	private static enum GroupFindType {
		ANY,
		PRIVATE,
		PUBLIC
	}
	
	private String getGroupFindTypeQuery(GroupFindType type) {
		if (type.equals(GroupFindType.PRIVATE))
			return " AND g.access = " + GroupAccess.SECRET.ordinal();
		else if (type.equals(GroupFindType.PUBLIC)) 
			return " AND g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal();
		else
			return "";
	}

	private Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status, GroupFindType groupFindType, boolean receivesOnly) {
		Query q;
		
		StringBuilder extraClause = new StringBuilder(getStatusClause(status));
		extraClause.append(getGroupFindTypeQuery(groupFindType));
		
		if (receivesOnly)
			extraClause.append(getReceivesClause());
		
		if (viewpoint.isOfUser(member) || viewpoint instanceof SystemViewpoint) {
			// Special case this for effiency
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + extraClause.toString()); 
		} else if (viewpoint instanceof UserViewpoint) {
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + " AND " + CAN_SEE + extraClause);
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());			
		} else {
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + " AND " + CAN_SEE_ANONYMOUS + extraClause);
		}
		q.setParameter("member", member);
		Set<Group> ret = new HashSet<Group>();
		for (Object o : q.getResultList()) {
			ret.add((Group) o);
		}
		return ret;
	}
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status) {
		return findRawGroups(viewpoint, member, status, GroupFindType.ANY, false);
	}
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member) {
		return findRawGroups(viewpoint, member, null, GroupFindType.ANY, false);
	}
	
	public Set<Group> findRawPublicGroups(Viewpoint viewpoint, User member) {
		return findRawGroups(viewpoint, member, null, GroupFindType.PUBLIC, false);
	}	
	
	public Set<Group> findRawPublicGroups(Viewpoint viewpoint, User member, MembershipStatus status) {
		return findRawGroups(viewpoint, member, status, GroupFindType.PUBLIC, false);
	}		

	public Set<Group> findRawPrivateGroups(Viewpoint viewpoint, User member) {
		return findRawGroups(viewpoint, member, null, GroupFindType.PRIVATE, false);
	}
	
	public Set<Group> findRawRecipientGroups(Viewpoint viewpoint, User member) {
		return findRawGroups(viewpoint, member, null, GroupFindType.ANY, true);
	}
 
	public void fixupGroupMemberships(User user) {
		Set<Group> groups = findRawGroups(SystemViewpoint.getInstance(), user);
		for (Group g : groups) {
			GroupMember member;
			try {
				member = getGroupMemberForUser(g, user, true);
				if (!(member.getMember() instanceof Account)) {
					// continue to fix up the other memberships rather than crashing completely
					logger.error("User " + user + " has not-fixed-up group member " + member + " in group " + g);
				}
			} catch (NotFoundException e) {
				// ignore
			}
		}
	}
	
	private Query buildFindGroupsQuery(Viewpoint viewpoint, User member, boolean isCount, MembershipStatus status, boolean allGroups, GroupFindType groupFindType) {
		Query q;		
		StringBuilder queryStr = new StringBuilder("SELECT ");
		boolean ownGroups = viewpoint.isOfUser(member);		
	
		if (isCount)
			queryStr.append("count(gm)");
		else
			queryStr.append("gm");
		
		queryStr.append(" FROM GroupMember gm, AccountClaim ac, Group g " +
        "WHERE ac.resource = gm.member AND ac.owner = :member AND g = gm.group ");
		queryStr.append(getStatusClause(status, allGroups));
		queryStr.append(getGroupFindTypeQuery(groupFindType));	
		
		if (ownGroups || viewpoint instanceof SystemViewpoint) {
			// Special case this for effiency
			q = em.createQuery(queryStr.toString());				
		} else if (viewpoint instanceof UserViewpoint) {
			queryStr.append(" AND ");
			queryStr.append(CAN_SEE);
			q = em.createQuery(queryStr.toString());				
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());			
		} else {
			queryStr.append(" AND ");
			queryStr.append(CAN_SEE_ANONYMOUS);
			q = em.createQuery(queryStr.toString());
		}
	
		q.setParameter("member", member);
		return q;
	}

	private Query buildFindGroupsQuery(Viewpoint viewpoint, Resource resource) {
		Query q;		

		StringBuilder queryStr = new StringBuilder(" SELECT gm FROM GroupMember gm, Group g " +
        "WHERE gm.member = :member AND g = gm.group ");
		
		if (viewpoint instanceof SystemViewpoint) {
			// Special case this for effiency
			q = em.createQuery(queryStr.toString());				
		} else if (viewpoint instanceof UserViewpoint) {
			queryStr.append(" AND ");
			queryStr.append(CAN_SEE);
			q = em.createQuery(queryStr.toString());				
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());			
		} else {
			queryStr.append(" AND ");
			queryStr.append(CAN_SEE_ANONYMOUS);
			q = em.createQuery(queryStr.toString());
		}
	
		q.setParameter("member", resource);
		return q;
	}
	
	public int findGroupsCount(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q = buildFindGroupsQuery(viewpoint, member, true, status, false, GroupFindType.ANY);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();			
	}
	
	public int findPublicGroupsCount(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q = buildFindGroupsQuery(viewpoint, member, true, status, false, GroupFindType.PUBLIC);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();			
	}	
	
	public Set<GroupView> findGroups(Viewpoint viewpoint, User member) {
		return findGroups(viewpoint, member, null);
	}
	
	public Set<GroupView> findGroups(Viewpoint viewpoint, User member, MembershipStatus status) {

		boolean ownGroups = viewpoint.isOfUser(member);
		
		Set<GroupView> result = new HashSet<GroupView>();
		
		Query q = buildFindGroupsQuery(viewpoint, member, false, status, false, GroupFindType.ANY);
		for (Object o : q.getResultList()) {
			GroupMember groupMember = (GroupMember)o;
			Set<PersonView> inviters  = new HashSet<PersonView>();
			
			// Only get the inviter information for viewing the viewpoint's own groups
			if (ownGroups) {
				if (groupMember.getStatus() == MembershipStatus.INVITED) {
					Set<User> adders = groupMember.getAdders();
					for(User adder : adders) {
						inviters.add(personViewer.getPersonView(viewpoint, adder));
					}
				}
			}
			
			result.add(new GroupView(viewpoint, groupMember.getGroup(), groupMember, inviters));
		}
		return result;	
	}
	
	public List<GroupMember> findGroups(Viewpoint viewpoint, Resource resource) {
		Query q;		
		AccountClaim ac = resource.getAccountClaim();
		if (ac != null) {
			// all the group memberships must have been fixed up once 
			// a user took over a resource
			q = buildFindGroupsQuery(viewpoint, ac.getOwner(), false, null, true, GroupFindType.ANY);						
		} else {
		    q = buildFindGroupsQuery(viewpoint, resource);
		}
		
		return TypeUtils.castList(GroupMember.class, q.getResultList());	
	}
	
	private static final String FIND_PUBLIC_GROUPS_QUERY = 
		"FROM Group g WHERE ";

	private static final String COUNT_PUBLIC_GROUPS_QUERY = 
		"SELECT COUNT(g) FROM Group g WHERE ";
	
	public void pagePublicGroups(Viewpoint viewpoint, Pageable<GroupView> pageable) {
		Query q;
		
		q = em.createQuery(FIND_PUBLIC_GROUPS_QUERY + CAN_SEE_ANONYMOUS 
				           + " ORDER BY LCASE(g.name)");
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<GroupView> groups = new ArrayList<GroupView>();
		for (Object o : q.getResultList()) {
			groups.add(new GroupView(viewpoint, (Group) o, null, null));
		}
		pageable.setResults(groups);
		pageable.setTotalCount(getPublicGroupCount());
	}
	
	public int getPublicGroupCount() {
		Query q = em.createQuery(COUNT_PUBLIC_GROUPS_QUERY + CAN_SEE_ANONYMOUS);
		return ((Number) q.getSingleResult()).intValue();
	}
	
	public void incrementGroupVersion(final Group group) {
//		While it isn't a big deal in practice, the implementation below is slightly
//		racy. The following would be better, but triggers a hibernate bug.
//
//		em.createQuery("UPDATE Group g set g.version = g.version + 1 WHERE g.id = :id")
//			.setParameter("id", groupId)
//			.executeUpdate();
//		
// 	       em.refresh(group);
				
		group.setVersion(group.getVersion() + 1);
	}
	
	// some usages of this don't necessarily trust the groupId to be valid, keep that in mind
	public Group lookupGroupById(Viewpoint viewpoint, String groupId) throws NotFoundException {
		Guid guid;
		
		try {
			guid = new Guid(groupId);
		} catch (ParseException e) {
			throw new NotFoundException("Invalid group ID " + groupId);
		}

		return lookupGroupById(viewpoint, guid);
	}
	
	public Group lookupGroupById(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		Group group = EJBUtil.lookupGuid(em, Group.class, guid);
		
		if (group.getAccess() == GroupAccess.SECRET &&
			!getViewerStatus(viewpoint, group).getCanSeeSecretGroup())
			throw new NotFoundException("No such group with ID " + guid + " for the given viewpoint");
		
		return group;
	}
	
	private static final String CONTACT_IS_MEMBER =
		" (EXISTS(SELECT cc FROM ContactClaim cc WHERE cc.contact = contact AND cc.resource = gm.member) OR" +
		" EXISTS(SELECT a2 FROM Account a2 WHERE a2.owner IN " +
		" (SELECT ac.owner FROM ContactClaim cc, AccountClaim ac WHERE cc.contact = contact AND ac.resource = cc.resource) " +
		" AND a2 = gm.member)) ";
	
	// this checks if the user can invite other people to a group
	private static final String CAN_SHARE_GROUP =
		" (vgm.status = " + MembershipStatus.ACTIVE.ordinal() + 
        " OR vgm.status = " + MembershipStatus.FOLLOWER.ordinal() + ") ";	
	
	// this filters out people who are group members or are already invited, as well
	// as filters out followers and people invited to follow if the viewer is a follower
	private static final String FIND_ADDABLE_CONTACTS_QUERY = 
		"SELECT contact from Account a, Contact contact, Group g, GroupMember vgm, AccountClaim ac " +
		"WHERE a.owner = :viewer AND contact.account = a AND " + 
			  "g.id = :groupid AND vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " + CAN_SHARE_GROUP +
			  "AND NOT EXISTS(SELECT gm FROM GroupMember gm " +
				         "WHERE gm.group = :groupid AND " + CONTACT_IS_MEMBER + " AND " +
				               "(gm.status >= " + MembershipStatus.INVITED.ordinal() +
				               " OR (vgm.status = " + MembershipStatus.FOLLOWER.ordinal() + 
				               " AND (gm.status = " + MembershipStatus.FOLLOWER.ordinal() +
				                      " OR gm.status = " + MembershipStatus.INVITED_TO_FOLLOW.ordinal() +
				                      "))))";

	public Set<PersonView> findAddableContacts(UserViewpoint viewpoint, User owner, String groupId, PersonViewExtra... extras) {
		Person viewer = viewpoint.getViewer();
		
		if (!viewpoint.isOfUser(owner))
			throw new RuntimeException("Not implemented");
		
		Query q = em.createQuery(FIND_ADDABLE_CONTACTS_QUERY);
		q.setParameter("viewer", viewer);
		q.setParameter("groupid", groupId);

		Set<PersonView> result = new HashSet<PersonView>();

		for (Object o: q.getResultList())
			result.add(personViewer.getPersonView(viewpoint, (Person)o, extras));
		
		return result;
	}	
	
	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String GROUP_MESSAGE_QUERY = "SELECT gm FROM GroupMessage gm WHERE gm.group = :group ";
	private static final String GROUP_MESSAGE_SELECT = " AND gm.id >= :lastSeenSerial ";
	private static final String GROUP_MESSAGE_ORDER = " ORDER BY gm.id";
	
	public List<GroupMessage> getGroupMessages(Group group, long lastSeenSerial) {
		List<?> messages = em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_SELECT + GROUP_MESSAGE_ORDER)
		.setParameter("group", group)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);
	}
	
	public List<GroupMessage> getNewestGroupMessages(Group group, int maxResults) {
		List<?> messages =  em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_ORDER + " DESC")
		.setParameter("group", group)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);		
	}
	
	public int getGroupMessageCount(Group group) {
		Query q = em.createQuery("SELECT COUNT(gm) FROM GroupMessage gm WHERE gm.group = :group")
			.setParameter("group", group);
		
		return ((Number)q.getSingleResult()).intValue();
	}

	public List<ChatMessageView> viewGroupMessages(List<GroupMessage> messages, Viewpoint viewpoint) {
		List<ChatMessageView> viewedMsgs = new ArrayList<ChatMessageView>();
		for (GroupMessage m : messages) {
			viewedMsgs.add(new ChatMessageView(m, personViewer.getPersonView(viewpoint, m.getFromUser())));
		}
		return viewedMsgs;
	}	
	
	public void addGroupMessage(Group group, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		GroupMessage groupMessage = new GroupMessage(group, fromUser, text, sentiment, timestamp);
		em.persist(groupMessage);

		notifier.onGroupMessageCreated(groupMessage);
	}

	public boolean canEditGroup(UserViewpoint viewpoint, Group group) {
		try {
			GroupMember member = getGroupMember(group, viewpoint.getViewer());
			return member.getStatus() == MembershipStatus.ACTIVE;
		} catch (NotFoundException e) {
			return false;
		}
	}

	public boolean isMember(Group group, User user) {
		try {
			GroupMember member = getGroupMemberForUser(group, user, false);
			assert member != null;
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	public void setStockPhoto(UserViewpoint viewpoint, Group group, String photo) {
		if (!canEditGroup(viewpoint, group))
			throw new RuntimeException("Only active members can edit a group");

		if (photo != null && !Validators.validateStockPhoto(photo))
			throw new RuntimeException("invalid stock photo name");
		
		group.setStockPhoto(photo);
	}

	public GroupView loadGroup(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		return getGroupView(viewpoint, lookupGroupById(viewpoint, guid));
	}
	
	public GroupView getGroupView(Viewpoint viewpoint, Group group) {
		GroupMember groupMember = null;
		// FIXME: add getGroupMemberByGroupId (or replace getGroupMember), so that
		// we only do one lookup in the database. Careful: need to propagate
		// the handling of REMOVED members from lookupGroupById to getGroupMember
		if (viewpoint instanceof UserViewpoint) {
			UserViewpoint userView = (UserViewpoint) viewpoint;
			try {
				groupMember = getGroupMember(viewpoint, group, userView.getViewer());
			} catch (NotFoundException e) {
				groupMember = new GroupMember(group, userView.getViewer().getAccount(), MembershipStatus.NONMEMBER);
			}
		} else {
			groupMember = new GroupMember(group, null, MembershipStatus.NONMEMBER);			
		}
				
		return new GroupView(viewpoint, group, groupMember, null);
	}

	public void acceptInvitation(UserViewpoint viewpoint, Group group) {
		// If you view a group you were invited to, you get added; you can leave again and then 
		// you enter the REMOVED state where you can re-add yourself but don't get auto-added.
		addMember(viewpoint.getViewer(), group, viewpoint.getViewer());
	}

	public GroupSearchResult searchGroups(Viewpoint viewpoint, String queryString) {
		final String[] fields = { "name", "description" };
		QueryParser queryParser = new MultiFieldQueryParser(fields, GroupIndexer.getInstance().createAnalyzer());
		queryParser.setDefaultOperator(Operator.AND);
		org.apache.lucene.search.Query query;
		try {
			query = queryParser.parse(queryString);
			
			Hits hits = GroupIndexer.getInstance().getSearcher().search(query);
			
			return new GroupSearchResult(hits);
			
		} catch (org.apache.lucene.queryParser.ParseException e) {
			return new GroupSearchResult("Can't parse query '" + queryString + "'");
		} catch (IOException e) {
			return new GroupSearchResult("System error while searching, please try again");
		}
	}

	public List<GroupView> getGroupSearchGroups(Viewpoint viewpoint, GroupSearchResult searchResult, int start, int count) {
		// The efficiency gain of having this wrapper is that we pass the real 
		// object to the method rather than the proxy; getGroups() can make many, 
		// many calls back against the GroupSystem
		return searchResult.getGroups(this, viewpoint, start, count);
	}

	public List<Guid> getAllGroupIds() {
		return TypeUtils.castList(Guid.class, em.createQuery("SELECT new com.dumbhippo.identity20.Guid(g.id) FROM Group g").getResultList());		
	}
}
