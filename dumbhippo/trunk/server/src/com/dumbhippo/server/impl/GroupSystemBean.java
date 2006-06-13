package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
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
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(GroupSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private TransactionRunner runner;
	
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
		User adder = null;
		GroupMember accountMember = null;
		Account account = user.getAccount();
		for (GroupMember member : allMembers) {
			// use our "most joined up" status
			if (member.getStatus().ordinal() > status.ordinal()) {
				status = member.getStatus();
				// prefer the "most joined" adder, but prefer any 
				// adder over null adder
				if (member.getAdder() != null)
					adder = member.getAdder();
			}
			
			if (member.getMember().equals(account)) {
				accountMember = member;
			} else {
				if (fixupExpected) {
					// get rid of anything that isn't an account member
					logger.debug("Removing group member {} in favor of account member", member);
					group.getMembers().remove(member);
					em.remove(member);
				} else {
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
			if (adder != null)
				accountMember.setAdder(adder);
			em.persist(accountMember);
			group.getMembers().add(accountMember);
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
			}
		}
		throw new NotFoundException("GroupMember for contact " + contact + " not found");
	}
	
	private GroupMember getGroupMember(Group group, Person person) throws NotFoundException {
		if (person instanceof User)
			return getGroupMemberForUser(group, (User)person, false);
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
	
	public void addMember(User adder, Group group, Person person) {
		GroupMember groupMember;
		try {
			// if person is a User then this will do fixups
			groupMember = getGroupMember(group, person);
		} catch (NotFoundException e) {
			groupMember = null;
		}
		boolean selfAdd = adder.equals(person); 
		
		MembershipStatus newStatus;
		if (selfAdd)
			newStatus = MembershipStatus.ACTIVE;
		else
			newStatus = MembershipStatus.INVITED;

		if (!(groupMember != null && selfAdd) &&
			group.getAccess() != GroupAccess.PUBLIC) {
			try {
				@SuppressWarnings("unused") GroupMember adderMember = getGroupMember(group, adder);
			} catch (NotFoundException e) {
				throw new IllegalArgumentException("invalid person adding member to group", e);
			}
		}
				
		if (groupMember != null) {
			switch (groupMember.getStatus()) {
			case NONMEMBER:
				throw new IllegalStateException();
			case REMOVED:
				if (!selfAdd)
					groupMember.setAdder(adder); // Mark adder for "please come back"
				break;
			case INVITED:
				if (selfAdd) {
					// accepting an invitation, add our inviter as a contact
					User previousAdder = groupMember.getAdder();
					if (previousAdder != null)
						identitySpider.addContactPerson(adder, previousAdder);
				} else {
					// already invited, do nothing
					return;
				}
				break;
			case ACTIVE:
				return; // Nothing to do
			}
			groupMember.setStatus(newStatus);
		} else {
			Resource resource = identitySpider.getBestResource(person);
			
			groupMember = new GroupMember(group, resource, newStatus);
			if (!selfAdd) { 
				groupMember.setAdder(adder);
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
		}
		
        LiveState.getInstance().queuePostTransactionUpdate(em, new GroupEvent(group.getGuid(), groupMember.getMember().getGuid(), GroupEvent.Type.MEMBERSHIP_CHANGE));
	}
	
	public void removeMember(User remover, Group group, Person person) {
		if (!remover.equals(person))
			throw new IllegalArgumentException("a group member can only be removed by themself");
		
		try {
			// note that getGroupMember() here does a fixup so we only have one GroupMember which 
			// canonically points to our account.
			GroupMember groupMember = getGroupMember(group, person);
			if (groupMember.getStatus() != MembershipStatus.REMOVED) {
				groupMember.setStatus(MembershipStatus.REMOVED);
				
		        LiveState.getInstance().queuePostTransactionUpdate(em, new GroupEvent(group.getGuid(), groupMember.getMember().getGuid(), GroupEvent.Type.MEMBERSHIP_CHANGE));						
			}
		} catch (NotFoundException e) {
			// nothing to do
		}
	}
	
	// We might want to allow REMOVED members full visibility, to give a 
	// "nomail" feel, but changing the visibility allows people to see
	// what the world sees and makes the operation more concrete
	// 
	// So, we have two query strings "Can see that the group exists" - 
	// this is needed to avoid the group page vanishing when you remove
	// yourself - and also "Can see private posts and members". There is 
	// no difference between the two for the anonymous viewer.
	private static final String CAN_SEE = 
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
		  "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
	              "WHERE vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " +
	              "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ")) ";
	private static final String CAN_SEE_GROUP =
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
		  "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
                  "WHERE vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " +
                  "vgm.status >= " + MembershipStatus.REMOVED.ordinal() + ")) ";
	private static final String CAN_SEE_ANONYMOUS = " g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " ";
	
	private String getStatusClause(MembershipStatus status) {
		if (status != null) {
			return " AND gm.status = " + status.ordinal();
		} else {
			return " AND gm.status >= " + MembershipStatus.INVITED.ordinal();
		}
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
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, PersonViewExtra... extras) {
		return getMembers(viewpoint, group, null, -1, extras);
	}
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, int maxResults, PersonViewExtra... extras) {
		
		// The subversion history has some code to try doing this with fewer queries; 
		// but for now keeping it simple
		
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, maxResults, status);
		if (resourceMembers.size() == 0)
			return Collections.emptySet();
		
		Set<PersonView> result = new HashSet<PersonView>();
		for (Resource r : resourceMembers) {
			result.add(identitySpider.getPersonView(viewpoint, r, PersonViewExtra.PRIMARY_RESOURCE, extras)); 
		}
		
		return result;
	}
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group) {
		return getUserMembers(viewpoint, group, null);
	}
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, -1, status);
		if (resourceMembers.size() == 0)
			return Collections.emptySet();
		
		Set<User> result = new HashSet<User>();
		for (Resource r : resourceMembers) {
			User user = identitySpider.getUser(r);
			if (user != null)
				result.add(user);
		}
		
		return result;
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	private static final String GET_GROUP_MEMBER_QUERY = 
		"SELECT gm FROM GroupMember gm, AccountClaim ac, Group g " +
        "WHERE gm.group = :group AND ac.resource = gm.member AND ac.owner = :member AND g = :group";
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member) throws NotFoundException {
		Query query;
		
		if (viewpoint.isOfUser(member) || viewpoint instanceof SystemViewpoint) {
			query = em.createQuery(GET_GROUP_MEMBER_QUERY);
		} else if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint)viewpoint).getViewer();
			query = em.createQuery(GET_GROUP_MEMBER_QUERY + " AND " + CAN_SEE);
			query.setParameter("viewer", viewer);
		} else  {
			query = em.createQuery(GET_GROUP_MEMBER_QUERY + " AND " + CAN_SEE_ANONYMOUS);			
		}
		query.setParameter("group", group);
		query.setParameter("member", member);
		
		try {
			return (GroupMember)query.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("GroupMember for resource " + member + " not found", e);
		}
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	private static final String FIND_RAW_GROUPS_QUERY = 
		"SELECT gm.group FROM GroupMember gm, AccountClaim ac, Group g " +
		"WHERE ac.resource = gm.member AND ac.owner = :member AND g = gm.group ";

	public Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q;
		
		String statusClause = getStatusClause(status);
		
		if (viewpoint.isOfUser(member) || viewpoint instanceof SystemViewpoint) {
			// Special case this for effiency
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + statusClause); 
		} else if (viewpoint instanceof UserViewpoint) {
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + " AND " + CAN_SEE + statusClause);
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());			
		} else {
			q = em.createQuery(FIND_RAW_GROUPS_QUERY + " AND " + CAN_SEE_ANONYMOUS + statusClause);
		}
		q.setParameter("member", member);
		Set<Group> ret = new HashSet<Group>();
		for (Object o : q.getResultList()) {
			ret.add((Group) o);
		}
		return ret;
	}
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member) {
		return findRawGroups(viewpoint, member, null);
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
	
	private Query buildFindGroupsQuery(Viewpoint viewpoint, User member, boolean isCount, MembershipStatus status) {
		Query q;		
		StringBuilder queryStr = new StringBuilder("SELECT ");
		boolean ownGroups = viewpoint.isOfUser(member);		
	
		if (isCount)
			queryStr.append("count(gm)");
		else
			queryStr.append("gm");
		
		queryStr.append(" FROM GroupMember gm, AccountClaim ac, Group g " +
        "WHERE ac.resource = gm.member AND ac.owner = :member AND g = gm.group ");
		queryStr.append(getStatusClause(status));
		
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
	
	public int findGroupsCount(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q = buildFindGroupsQuery(viewpoint, member, true, status);
		Object result = q.getSingleResult();
		return ((Number) result).intValue();			
	}
	
	public Set<GroupView> findGroups(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q = buildFindGroupsQuery(viewpoint, member, false, status);
		boolean ownGroups = viewpoint.isOfUser(member);
		
		Set<GroupView> result = new HashSet<GroupView>();
		for (Object o : q.getResultList()) {
			GroupMember groupMember = (GroupMember)o;
			PersonView inviter  = null;
			
			// Only get the inviter information for viewing the viewpoint's own groups
			if (ownGroups) {
				if (groupMember.getStatus() == MembershipStatus.INVITED) {
					User adder = groupMember.getAdder();
					if (adder != null)
						inviter = identitySpider.getPersonView(viewpoint, adder);
				}
			}
			
			result.add(new GroupView(groupMember.getGroup(), groupMember, inviter));
		}
		return result;	
	}
	
	public int incrementGroupVersion(final String groupId) {
		try {
			return runner.runTaskInNewTransaction(new Callable<Integer>() {

				public Integer call() {
//				While it isn't a big deal in practice, the implementation below is slightly
//				racy. The following would be better, but triggers a hibernate bug.

//				em.createQuery("UPDATE Group g set g.version = g.version + 1 WHERE g.id = :id")
//				.setParameter("id", groupId)
//				.executeUpdate();
				
//				return em.find(Group.class, groupId).getVersion();

					Group group = em.find(Group.class, groupId);
					int newVersion = group.getVersion() + 1;
					
					group.setVersion(newVersion);
					
					return newVersion;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return 0; // not reached
		}
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	private static final String LOOKUP_GROUP_QUERY = "SELECT g FROM Group g where g.id = :groupId";

	public Group lookupGroupById(Viewpoint viewpoint, String groupId) throws NotFoundException {
		Query query;
		
		if (viewpoint instanceof SystemViewpoint) {
			query = em.createQuery(LOOKUP_GROUP_QUERY);
		} else if (viewpoint instanceof UserViewpoint) {
			User viewer = ((UserViewpoint)viewpoint).getViewer();
			query = em.createQuery(LOOKUP_GROUP_QUERY + " AND " + CAN_SEE_GROUP);
			query.setParameter("viewer", viewer);
		} else {
			query = em.createQuery(LOOKUP_GROUP_QUERY + " AND " + CAN_SEE_ANONYMOUS);
		}
		query.setParameter("groupId", groupId);
		
		try {
			return (Group)query.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("No such group with ID " + groupId + " for the given viewpoint", e);
		}
	}
	
	public Group lookupGroupById(Viewpoint viewpoint, Guid guid) throws NotFoundException {
		return lookupGroupById(viewpoint, guid.toString());
	}
	
	private static final String CONTACT_IS_MEMBER =
		" EXISTS(SELECT cc FROM ContactClaim cc WHERE cc.contact = contact AND cc.resource = gm.member) ";
	
	private static final String FIND_ADDABLE_CONTACTS_QUERY = 
		"SELECT contact from Account a, Contact contact, Group g " +
		"WHERE a.owner = :viewer AND contact MEMBER OF a.contacts AND " + 
			  "g.id = :groupid AND " + CAN_SEE_GROUP + " AND " + 
			  "NOT EXISTS(SELECT gm FROM GroupMember gm " +
				         "WHERE gm.group = :groupid AND " + CONTACT_IS_MEMBER + " AND " +
				               "gm.status >= " + MembershipStatus.INVITED.ordinal() + ")";

	public Set<PersonView> findAddableContacts(UserViewpoint viewpoint, User owner, String groupId, PersonViewExtra... extras) {
		Person viewer = viewpoint.getViewer();
		
		if (!viewpoint.isOfUser(owner))
			throw new RuntimeException("Not implemented");
		
		Query q = em.createQuery(FIND_ADDABLE_CONTACTS_QUERY);
		q.setParameter("viewer", viewer);
		q.setParameter("groupid", groupId);

		Set<PersonView> result = new HashSet<PersonView>();

		for (Object o: q.getResultList())
			result.add(identitySpider.getPersonView(viewpoint, (Person)o, extras));
		
		return result;
	}	
	
	private static final String GROUP_MESSAGE_QUERY = "SELECT pm from GroupMessage pm WHERE pm.group = :group";
	private static final String GROUP_MESSAGE_ORDER = " ORDER BY pm.timestamp";
	
	public List<GroupMessage> getGroupMessages(Group group) {
		List<?> messages =  em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_ORDER)
		.setParameter("group", group)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);
	}
	
	public void addGroupMessage(Group group, User fromUser, String text, Date timestamp, int serial) {
		// we use serial = -1 in other places in the system to designate a message that contains
		// the group description, but we never add this type of message to the database
		if (serial < 0) 
			throw new IllegalArgumentException("Negative serial");
		
		GroupMessage groupMessage = new GroupMessage(group, fromUser, text, timestamp, serial);
		em.persist(groupMessage);
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
}
