package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupSystemRemote;

@Stateless
public class GroupSystemBean implements GroupSystem, GroupSystemRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
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
		if (!group.getMembers().contains(adder)) {
			throw new IllegalArgumentException("invalid person adding member to group");
		}		
		group.addMember(person);
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
