package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;

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
	
	public Set<PersonInfo> getMemberInfos(Person viewpoint, String groupId) {
		Group group;
		
		try {
			group = identitySpider.lookupGuidString(Group.class, groupId);
		} catch (ParseException e) {
        	throw new IllegalArgumentException(e);
        } catch (IdentitySpider.GuidNotFoundException e) {
        	throw new IllegalArgumentException(e);
        }
		
		Set<PersonInfo> result = new HashSet<PersonInfo>();
		for (Person p : group.getMembers()) 
			result.add(new PersonInfo(identitySpider, viewpoint, p));
		
		return result;
	}
	
	public boolean isMember(Group group, Person member) {
		Query query = em.createQuery("select count(g) from Group g where g = :group and :member in elements(g.members)");
		query.setParameter("group", group);
		query.setParameter("member", member);
		
		// This is a bug in the Hibernate EJB3 implementation; a count query should
		// return Long not Integer according to the spec.
		return (Integer)query.getSingleResult() > 0;
	}

	public Set<Group> findGroups(Person viewpoint) {
		Query q;
		q = em.createQuery("from Group g where :personid in elements(g.members)");
		q.setParameter("personid", viewpoint);		
		Set<Group> ret = new HashSet<Group>();
		for (Object o : q.getResultList()) {
			ret.add((Group) o);
		}
		return ret;
	}
}
