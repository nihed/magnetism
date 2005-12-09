package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(GroupSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
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
	
	private GroupMember getGroupMemberForUser(Group group, User user) {
		try {
			return (GroupMember)em.createQuery(GET_GROUP_MEMBER_FOR_USER_QUERY)
				.setParameter("group", group)
				.setParameter("user", user)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	static final String GET_GROUP_MEMBER_FOR_CONTACT_QUERY =
		"SELECT gm FROM GroupMember gm, ContactClaim cc " +
		"WHERE gm.group = :group AND cc.resource = gm.member AND cc.contact = :contact";
	
	private GroupMember getGroupMemberForContact(Group group, Contact contact) {
		try {
			return (GroupMember)em.createQuery(GET_GROUP_MEMBER_FOR_CONTACT_QUERY)
				.setParameter("group", group)
				.setParameter("contact", contact)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private GroupMember getGroupMember(Group group, Person person) {
		if (person instanceof User)
			return getGroupMemberForUser(group, (User)person);
		else
			return getGroupMemberForContact(group, (Contact)person);
	}

	public void addMember(User adder, Group group, Person person) {
		GroupMember groupMember = getGroupMember(group, person);
		boolean selfAdd = adder.equals(person); 
		
		MembershipStatus newStatus;
		if (selfAdd)
			newStatus = MembershipStatus.ACTIVE;
		else
			newStatus = MembershipStatus.INVITED;

		if (!(groupMember != null && selfAdd) &&
			group.getAccess() != GroupAccess.PUBLIC &&
			getGroupMember(group, adder) == null) {
			throw new IllegalArgumentException("invalid person adding member to group");
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
		}
	}
	
	public void removeMember(User remover, Group group, Person person) {
		if (!remover.equals(person))
			throw new IllegalArgumentException("a group member can only be removed by themself");
		
		GroupMember groupMember = getGroupMember(group, person);
		if (groupMember != null)
			groupMember.setStatus(MembershipStatus.REMOVED);
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
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, PersonViewExtra... extras) {
		return getMembers(viewpoint, group, null, extras);
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_CONTACT_MEMBERS_QUERY = 
		"SELECT cc FROM Group g, GroupMember gm, ContactClaim cc " +
		"WHERE gm.group = :group AND g = :group AND cc.resource = gm.member AND cc.account = :account";
	
	private String getStatusClause(MembershipStatus status) {
		if (status != null) {
			return " AND gm.status = " + status.ordinal();
		} else {
			return " AND gm.status >= " + MembershipStatus.INVITED.ordinal();
		}
	}
	
	private List<ContactClaim> getContactMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		String statusClause = getStatusClause(status);
		User viewer = viewpoint.getViewer();
		
		if (viewer == null)
			return Collections.emptyList();

		@SuppressWarnings("unchecked")
		List<ContactClaim> result = em.createQuery(GET_CONTACT_MEMBERS_QUERY + " AND " + CAN_SEE + statusClause)
			.setParameter("viewer", viewer)
			.setParameter("account", viewer.getAccount())			
			.setParameter("group", group)
			.getResultList();
			
		return result;
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_ACCOUNT_MEMBERS_QUERY = 
		"SELECT ac FROM Group g, GroupMember gm, AccountClaim ac " +
		"WHERE gm.group = :group AND g = :group AND ac.resource = gm.member";
		
	private List<AccountClaim> getAccountMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		String statusClause = getStatusClause(status);
		User viewer = viewpoint.getViewer();

		Query q;
		if (viewer == null) {
			q = em.createQuery(GET_ACCOUNT_MEMBERS_QUERY + " AND " + CAN_SEE_ANONYMOUS + statusClause);
		} else {
			q = em.createQuery(GET_ACCOUNT_MEMBERS_QUERY + " AND " + CAN_SEE + statusClause)
		    	.setParameter("viewer", viewer);
		}

		@SuppressWarnings("unchecked")
		List<AccountClaim> result = q
			.setParameter("group", group)
			.getResultList();
		
		return result;
	}

	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_RESOURCE_MEMBERS_QUERY = 
		"SELECT gm.member FROM GroupMember gm, Group g " +
		"WHERE gm.group = :group AND g = :group";
		
	private List<Resource> getResourceMembers(Viewpoint viewpoint, Group group, MembershipStatus status) {
		String statusClause = getStatusClause(status);
		User viewer = viewpoint.getViewer();

		Query q;
		if (viewer == null) {
			q = em.createQuery(GET_RESOURCE_MEMBERS_QUERY + " AND " + CAN_SEE_ANONYMOUS + statusClause);
		} else {
			q = em.createQuery(GET_RESOURCE_MEMBERS_QUERY + " AND " + CAN_SEE + statusClause)
		    	.setParameter("viewer", viewer);
		}

		@SuppressWarnings("unchecked")
		List<Resource> result = q
			.setParameter("group", group)
			.getResultList();
			
		return result;
	}

	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, PersonViewExtra... extras) {
		Map<Resource,PersonView> members = new HashMap<Resource,PersonView>();
		
		// If EJB-QL was more advanced, we could do this as a single query, but
		// failing that, we need to query for members that we can map to a contact,
		// members that we can map to a user/account, and raw members separately.
		//
		// We favor user members over contact members for now since they work
		// a bit better in the user interface. Really, we should be taking the merge
		// of the two but we'll leave that for another pass.

		// Get this first to allow us to optimize out the case of an empty result
		// this is common when status is non-NULL.
		List<Resource> resourceMembers = getResourceMembers(viewpoint, group, status);
		if (resourceMembers.size() == 0)
			return Collections.emptySet();
		
		for (AccountClaim ac : getAccountMembers(viewpoint, group, status)) {
			members.put(ac.getResource(), identitySpider.getPersonView(viewpoint, ac.getOwner(), extras));
		}
		for (ContactClaim cc : getContactMembers(viewpoint, group, status)) {
			if (!members.containsKey(cc.getResource()))
				members.put(cc.getResource(), identitySpider.getPersonView(viewpoint, cc.getContact(), extras));
		}
		for (Resource r : resourceMembers) {
			PersonView pv = new PersonView(null, null);
			// this covers any "extras" that may have been requested
			pv.addAllResources(Collections.singleton(r));
		}
		
		Set<PersonView> result = new HashSet<PersonView>();
		result.addAll(members.values());
		
		return result;
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_GROUP_MEMBER_QUERY = 
		"SELECT gm FROM GroupMember gm, AccountClaim ac, Group g " +
        "WHERE gm.group = :group AND ac.resource = gm.member AND ac.owner = :member AND g = :group";
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member) {
		Person viewer = viewpoint.getViewer();
		Query query;
		
		if (member.equals(viewer)) {
			query = em.createQuery(GET_GROUP_MEMBER_QUERY);
		} else if (viewer == null) {
			query = em.createQuery(GET_GROUP_MEMBER_QUERY + " AND " + CAN_SEE_ANONYMOUS);			
		} else {
			query = em.createQuery(GET_GROUP_MEMBER_QUERY + " AND " + CAN_SEE);
			query.setParameter("viewer", viewpoint.getViewer());
		}
		query.setParameter("group", group);
		query.setParameter("member", member);
		
		try {
			return (GroupMember)query.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
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

	public Set<Group> findRawGroups(Viewpoint viewpoint, Person member, MembershipStatus status) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		String baseQuery;
		if (member instanceof User)
			baseQuery = FIND_RAW_GROUPS_BY_USER_QUERY;
		else
			baseQuery = FIND_RAW_GROUPS_BY_CONTACT_QUERY;
		
		String statusClause = getStatusClause(status);
		
		if (member.equals(viewpoint.getViewer())) {
			// Special case this for effiency
			q = em.createQuery(baseQuery + statusClause); 
		} else if (viewer == null) {
			q = em.createQuery(baseQuery + " AND " + CAN_SEE_ANONYMOUS + statusClause);
		} else {
			q = em.createQuery(baseQuery + " AND " + CAN_SEE + statusClause);
			q.setParameter("viewer", viewer);			
		}
		q.setParameter("member", member);
		Set<Group> ret = new HashSet<Group>();
		for (Object o : q.getResultList()) {
			ret.add((Group) o);
		}
		return ret;
	}
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, Person member) {
		return findRawGroups(viewpoint, member, null);
	}

	static final String FIND_GROUPS_QUERY = 
		"SELECT gm FROM GroupMember gm, AccountClaim ac " +
        "WHERE ac.resource = gm.member AND ac.owner = :member AND " +
        "      gm.status  >= " + MembershipStatus.INVITED.ordinal();

	public Set<GroupView> findGroups(Viewpoint viewpoint, Person member) {
		Query q;
		
		// See the usage of PostingBoardBean.VISIBLE_GROUP_WITH_MEMBER for
		// how to implement the general case
		if (!member.equals(viewpoint.getViewer()))
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
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)	
	public int incrementGroupVersion(String groupId) {
//		While it isn't a big deal in practice, the implementation below is slightly
//		racy. The following would be better, but triggers a hibernate bug.
//
//		em.createQuery("UPDATE Group g set g.version = g.version + 1 WHERE g.id = :id")
//		.setParameter("id", groupId)
//		.executeUpdate();
//		
//		return em.find(Group.class, groupId).getVersion();
//
		Group group = em.find(Group.class, groupId);
		int newVersion = group.getVersion() + 1;

		group.setVersion(newVersion);
		
		return newVersion;
		
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String LOOKUP_GROUP_QUERY = "SELECT g FROM Group g where g.id = :groupId";

	public Group lookupGroupById(Viewpoint viewpoint, String groupId) {
		Person viewer = viewpoint.getViewer();
		Query query;
		
		if (viewer == null) {
			query = em.createQuery(LOOKUP_GROUP_QUERY + " AND " + CAN_SEE_ANONYMOUS);			
		} else {
			query = em.createQuery(LOOKUP_GROUP_QUERY + " AND " + CAN_SEE_GROUP);
			query.setParameter("viewer", viewpoint.getViewer());
		}
		query.setParameter("groupId", groupId);
		
		try {
			return (Group)query.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
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

	public Set<PersonView> findAddableContacts(Viewpoint viewpoint, User owner, String groupId, PersonViewExtra... extras) {
		Person viewer = viewpoint.getViewer();
		
		if (!owner.equals(viewer))
			throw new RuntimeException("Not implemented");
		
		Query q = em.createQuery(FIND_ADDABLE_CONTACTS_QUERY);
		q.setParameter("viewer", viewer);
		q.setParameter("groupid", groupId);

		Set<PersonView> result = new HashSet<PersonView>();

		for (Object o: q.getResultList())
			result.add(identitySpider.getPersonView(viewpoint, (Person)o, extras));
		
		return result;
	}
}
