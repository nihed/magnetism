package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(GroupSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	public Group createGroup(Person creator, String name) {	
		Group g = new Group(name);
		GroupMember groupMember = new GroupMember(g, creator, MembershipStatus.ACTIVE);
		em.persist(groupMember);
		g.getMembers().add(groupMember);
		em.persist(g);
		return g;
	}
	
	public void deleteGroup(Person deleter, Group group) {
		if (!group.getMembers().contains(deleter)) {
			throw new IllegalArgumentException("invalid person deleting group");
		}
	}
	
	private GroupMember getGroupMember(Group group, Person person) {
		try {
			Query q = em.createQuery("SELECT gm FROM GroupMember gm " +
					                 "WHERE gm.group = :group AND gm.member = :person");
			q.setParameter("group", group);
			q.setParameter("person", person);
			
			return (GroupMember)q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}

	public void addMember(Person adder, Group group, Person person) {
		GroupMember groupMember = getGroupMember(group, person);
		boolean selfAdd = adder.equals(person); 
		
		MembershipStatus newStatus;
		if (selfAdd)
			newStatus = MembershipStatus.INVITED;
		else
			newStatus = MembershipStatus.ACTIVE;

		if (!(groupMember != null && selfAdd) &&
			group.getAccess() != GroupAccess.PUBLIC &&
			getGroupMember(group, adder) == null) {
			throw new IllegalArgumentException("invalid person adding member to group");
		}	
				
		if (groupMember != null) {
			groupMember.setStatus(newStatus);
		} else {
			groupMember = new GroupMember(group, person, newStatus);
			em.persist(groupMember);
			group.getMembers().add(groupMember);
			em.persist(group);
		}
	}
	
	public void removeMember(Person remover, Group group, Person person) {
		if (!remover.equals(person))
			throw new IllegalArgumentException("a group member can only be removed by themself");
		
		GroupMember groupMember = getGroupMember(group, person);
		if (groupMember != null)
			groupMember.setStatus(MembershipStatus.REMOVED);
	}
	
	// We might want to allow REMOVED members full visibility, to give a 
	// "nomail" feel, but changing the visibility allows people to see
	// what the world sees and makes the operation more concrete
	static final String CAN_SEE = " (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
					                "EXISTS (SELECT vgm FROM GroupMember vgm " +
	                                         "WHERE vgm.group = g AND vgm.member = :viewer AND " +
	                                             "vgm.status >= " + MembershipStatus.INVITED.ordinal() + ")) ";
	static final String CAN_SEE_ANONYMOUS = " g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " ";
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String GET_MEMBERS_QUERY = "SELECT gm.member from GroupMember gm, Group g " +
	                                        "WHERE gm.group = :group AND g = :group ";
		
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		if (viewer == null) {
			q = em.createQuery(GET_MEMBERS_QUERY + " AND " + CAN_SEE_ANONYMOUS);
		} else {
			q = em.createQuery(GET_MEMBERS_QUERY + " AND " + CAN_SEE);
			q.setParameter("viewer", viewer);			
		}
		q.setParameter("group", group);

		Set<PersonView> result = new HashSet<PersonView>();
		for (Object o : q.getResultList()) 
			result.add(identitySpider.getPersonView(viewpoint, (Person)o));
		
		return result;
	}
	
	// The selection of Group is only needed for the CAN_SEE checks
	static final String MEMBERSHIP_STATUS_QUERY = "SELECT gm FROM GroupMember gm, Group g " +
                                                  "WHERE gm.group = :group AND gm.member = :member AND g = :group";
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, Person member) {
		Person viewer = viewpoint.getViewer();
		Query query;
		
		if (member.equals(viewer)) {
			query = em.createQuery(MEMBERSHIP_STATUS_QUERY);
		} else if (viewer == null) {
			query = em.createQuery(MEMBERSHIP_STATUS_QUERY + " AND " + CAN_SEE_ANONYMOUS);			
		} else {
			query = em.createQuery(MEMBERSHIP_STATUS_QUERY + " AND " + CAN_SEE);
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
	static final String FIND_GROUPS_QUERY = "SELECT gm.group FROM GroupMember gm, Group g " +
                                            "WHERE gm.member = :member AND g = gm.group";

	public Set<Group> findGroups(Viewpoint viewpoint, Person member) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		if (member.equals(viewpoint.getViewer())) {
			// Special case this for effiency
			q = em.createQuery(FIND_GROUPS_QUERY); 
		} else if (viewer == null) {
			q = em.createQuery(FIND_GROUPS_QUERY + " AND " + CAN_SEE_ANONYMOUS);
		} else {
			q = em.createQuery(FIND_GROUPS_QUERY + " AND " + CAN_SEE);
			q.setParameter("viewer", viewer);			
		}
		q.setParameter("member", member);
		Set<Group> ret = new HashSet<Group>();
		for (Object o : q.getResultList()) {
			ret.add((Group) o);
		}
		return ret;
	}
}
