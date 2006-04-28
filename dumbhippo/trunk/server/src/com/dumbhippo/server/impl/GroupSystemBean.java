package com.dumbhippo.server.impl;

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
import com.dumbhippo.live.GroupMembershipChangeEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.Contact;
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
	
	public Group createGroup(User creator, String name, GroupAccess access) {
		if (creator == null)
			throw new IllegalArgumentException("null group creator");
		
		Group g = new Group(name, access);
		
		em.persist(g);
		
		GroupMember groupMember = new GroupMember(g, creator.getAccount(), MembershipStatus.ACTIVE);
		
		em.persist(groupMember);
		
		// Fix up the inverse side of the mapping
		g.getMembers().add(groupMember);

		return g;
	}
	
	public void deleteGroup(User deleter, Group group) {
		// FIXME how can this even imagine working? getMembers() returns a set of GroupMember
		if (!group.getMembers().contains(deleter)) {
			throw new IllegalArgumentException("invalid person deleting group");
		}
	}
	
	static final String GET_GROUP_MEMBER_FOR_USER_QUERY =
		"SELECT gm FROM GroupMember gm, AccountClaim ac " +
		"WHERE gm.group = :group AND ac.resource = gm.member AND ac.owner = :user";
	
	private GroupMember getGroupMemberForUser(Group group, User user) throws NotFoundException {
		try {
			return (GroupMember)em.createQuery(GET_GROUP_MEMBER_FOR_USER_QUERY)
				.setParameter("group", group)
				.setParameter("user", user)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("GroupMember for user " + user + " not found", e);
		}
	}
	
	static final String GET_GROUP_MEMBER_FOR_CONTACT_QUERY =
		"SELECT gm FROM GroupMember gm, ContactClaim cc " +
		"WHERE gm.group = :group AND cc.resource = gm.member AND cc.contact = :contact";
	
	private GroupMember getGroupMemberForContact(Group group, Contact contact) throws NotFoundException {
		try {
			return (GroupMember)em.createQuery(GET_GROUP_MEMBER_FOR_CONTACT_QUERY)
				.setParameter("group", group)
				.setParameter("contact", contact)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("GroupMember for contact " + contact + " not found", e);
		}
	}
	
	private GroupMember getGroupMember(Group group, Person person) throws NotFoundException {
		if (person instanceof User)
			return getGroupMemberForUser(group, (User)person);
		else
			return getGroupMemberForContact(group, (Contact)person);
	}

	static final String GET_GROUP_MEMBER_FOR_RESOURCE_QUERY =
		"SELECT gm FROM GroupMember gm " +
		"WHERE gm.group = :group AND gm.member = :resource";
	
	public GroupMember getGroupMember(Group group, Resource member) throws NotFoundException {
		try {
			return (GroupMember)em.createQuery(GET_GROUP_MEMBER_FOR_RESOURCE_QUERY)
				.setParameter("group", group)
				.setParameter("resource", member)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("GroupMember for resource " + member + " not found", e);
		}
	}
	
	public void addMember(User adder, Group group, Person person) {
		GroupMember groupMember;
		try {
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
			if (!selfAdd) 
				groupMember.setAdder(adder);
			em.persist(groupMember);
			group.getMembers().add(groupMember);
			em.persist(group);
			
	        LiveState.getInstance().queueUpdate(new GroupMembershipChangeEvent(group.getGuid(), true));		
		}
	}
	
	public void removeMember(User remover, Group group, Person person) {
		if (!remover.equals(person))
			throw new IllegalArgumentException("a group member can only be removed by themself");
		
		try {
			GroupMember groupMember = getGroupMember(group, person);
			if (groupMember.getStatus() != MembershipStatus.REMOVED) {
				groupMember.setStatus(MembershipStatus.REMOVED);
				
		        LiveState.getInstance().queueUpdate(new GroupMembershipChangeEvent(group.getGuid(), false));						
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
	static final String CAN_SEE = 
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
		  "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
	              "WHERE vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " +
	              "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ")) ";
	static final String CAN_SEE_GROUP =
		" (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
		  "EXISTS (SELECT vgm FROM GroupMember vgm, AccountClaim ac " +
                  "WHERE vgm.group = g AND ac.resource = vgm.member AND ac.owner = :viewer AND " +
                  "vgm.status >= " + MembershipStatus.REMOVED.ordinal() + ")) ";
	static final String CAN_SEE_ANONYMOUS = " g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " ";
	
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
		
	private List<Resource> getResourceMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		Query q = buildGetResourceMembersQuery(viewpoint, group, status, false);
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
		return getMembers(viewpoint, group, null, extras);
	}
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, PersonViewExtra... extras) {
		
		// The subversion history has some code to try doing this with fewer queries; 
		// but for now keeping it simple
		
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, status);
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
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, status);
		if (resourceMembers.size() == 0)
			return Collections.emptySet();
		
		Set<User> result = new HashSet<User>();
		for (Resource r : resourceMembers) {
			User user = identitySpider.getUser(r);
			result.add(user);
		}
		
		return result;
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_GROUP_MEMBER_QUERY = 
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
	static final String FIND_RAW_GROUPS_BY_USER_QUERY = 
		"SELECT gm.group FROM GroupMember gm, AccountClaim ac, Group g " +
		"WHERE ac.resource = gm.member AND ac.owner = :member AND g = gm.group AND " +
		"      gm.status  >= " + MembershipStatus.INVITED.ordinal();

	static final String FIND_RAW_GROUPS_BY_CONTACT_QUERY = 
		"SELECT gm.group FROM GroupMember gm, ContactClaim cc, Group g " +
		"WHERE cc.resource = gm.member AND cc.contact = :member AND g = gm.group AND " +
		"      gm.status  >= " + MembershipStatus.INVITED.ordinal();

	public Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status) {
		Query q;
		
		String baseQuery;
		if (member instanceof User)
			baseQuery = FIND_RAW_GROUPS_BY_USER_QUERY;
		else
			baseQuery = FIND_RAW_GROUPS_BY_CONTACT_QUERY;
		
		String statusClause = getStatusClause(status);
		
		if (viewpoint.isOfUser(member) || viewpoint instanceof SystemViewpoint) {
			// Special case this for effiency
			q = em.createQuery(baseQuery + statusClause); 
		} else if (viewpoint instanceof UserViewpoint) {
			q = em.createQuery(baseQuery + " AND " + CAN_SEE + statusClause);
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());			
		} else {
			q = em.createQuery(baseQuery + " AND " + CAN_SEE_ANONYMOUS + statusClause);
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

	static final String FIND_GROUPS_QUERY = 
		"SELECT gm FROM GroupMember gm, AccountClaim ac " +
        "WHERE ac.resource = gm.member AND ac.owner = :member AND " +
        "      gm.status  >= " + MembershipStatus.INVITED.ordinal();

	public Set<GroupView> findGroups(UserViewpoint viewpoint, User member) {
		Query q;
		
		// See the usage of PostingBoardBean.VISIBLE_GROUP_WITH_MEMBER for
		// how to implement the general case
		if (!viewpoint.isOfUser(member))
			throw new RuntimeException("Not implemented");
		
		q = em.createQuery(FIND_GROUPS_QUERY); 
		q.setParameter("member", member);
		Set<GroupView> result = new HashSet<GroupView>();
		for (Object o : q.getResultList()) {
			GroupMember groupMember = (GroupMember)o;
			PersonView inviter  = null;
			
			if (groupMember.getStatus() == MembershipStatus.INVITED) {
				User adder = groupMember.getAdder();
				if (adder != null)
					inviter = identitySpider.getPersonView(viewpoint, adder);
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
	static final String LOOKUP_GROUP_QUERY = "SELECT g FROM Group g where g.id = :groupId";

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
	
	static final String CONTACT_IS_MEMBER =
		" EXISTS(SELECT cc FROM ContactClaim cc WHERE cc.contact = contact AND cc.resource = gm.member) ";
	
	static final String FIND_ADDABLE_CONTACTS_QUERY = 
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
}
