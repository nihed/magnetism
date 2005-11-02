package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	public Group createGroup(Person creator, String name) {	
		Group g = new Group(name);
		g.addMember(creator);
		em.persist(g);
		return g;
	}
	
	public void deleteGroup(Person deleter, Group group) {
		if (!group.getMembers().contains(deleter)) {
			throw new IllegalArgumentException("invalid person deleting group");
		}
	}

	public void addMember(Person adder, Group group, Person person) {
		// 	Enable self-addition temporarily
//		if (!group.getMembers().contains(adder)) {
//			throw new IllegalArgumentException("invalid person adding member to group");
//		}		
		group.addMember(person);
	}
	
	public void removeMember(Person remover, Group group, Person person) {
		if (!remover.getId().equals(person.getId()))
			throw new IllegalArgumentException("a group member can only be removed by themself");
		
		group.removeMember(person);
	}
	
	static final String CAN_SEE = " (g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " + 
					                ":viewer MEMBER OF g.members) ";
	static final String CAN_SEE_ANONYMOUS = " g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " ";
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		if (viewer == null) {
			q = em.createQuery("SELECT person FROM Group g, Person person " +
							       "WHERE g = :group AND " +
								         "person MEMBER OF g.members AND " +
					                      CAN_SEE_ANONYMOUS);
		} else {
			q = em.createQuery("SELECT person FROM Group g, Person person " +
					               "WHERE g = :group AND " +
								         "person MEMBER OF g.members AND " +
		                                  CAN_SEE);
			q.setParameter("viewer", viewer);			
		}
		q.setParameter("group", group);

		Set<PersonView> result = new HashSet<PersonView>();
		for (Object o : q.getResultList()) 
			result.add(identitySpider.getPersonView(viewpoint, (Person)o));
		
		return result;
	}
	
	public boolean isMember(Viewpoint viewpoint, Group group, Person member) {
		Person viewer = viewpoint.getViewer();
		Query query;
		
		if (member.equals(viewer)) {
			query = em.createQuery("SELECT COUNT(g) from Group g " +
			                           "WHERE g = :group AND " + 
			                                 ":member MEMBER OF g.members");			
		} else if (viewer == null) {
			query = em.createQuery("SELECT COUNT(g) from Group g " +
	                   "WHERE g = :group AND " + 
                       ":member MEMBER OF g.members AND " +
                        CAN_SEE_ANONYMOUS);			
		} else {
			query = em.createQuery("SELECT COUNT(g) from Group g " +
	                   "WHERE g = :group AND " + 
                          ":member MEMBER OF g.members AND " +
                           CAN_SEE);
			query.setParameter("viewer", viewpoint.getViewer());
			
		}
		query.setParameter("group", group);
		query.setParameter("member", member);
		
		// This is a bug in the Hibernate EJB3 implementation; a count query should
		// return Long not Integer according to the spec.
		return (Integer)query.getSingleResult() > 0;
	}

	public Set<Group> findGroups(Viewpoint viewpoint, Person member) {
		Person viewer = viewpoint.getViewer();
		Query q;
		
		if (member.equals(viewpoint.getViewer())) {
			// Special case this for effiency
			q = em.createQuery("SELECT g FROM Group g WHERE :member MEMBER OF g.members");
		} else if (viewer == null) {
			q = em.createQuery("SELECT g FROM Group g " +
					               "WHERE :member MEMBER OF g.members AND " +
					               CAN_SEE_ANONYMOUS);
		} else {
			q = em.createQuery("SELECT g FROM Group g " +
					               "WHERE :member MEMBER OF g.members AND " +
		                           CAN_SEE);
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
