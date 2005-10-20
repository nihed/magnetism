package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AbstractLoginRequired;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;

@SuppressWarnings("serial")
public class GroupSystemBean extends AbstractLoginRequired implements GroupSystem {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	public Group createGroup(String name) {
		Person creator = getLoggedInUser(identitySpider);		
		Group g = new Group(name);
		g.addMember(creator);
		em.persist(g);
		return g;
	}
	
	public void deleteGroup(Group group) {
		Person deleter = getLoggedInUser(identitySpider);
		if (!group.getMembers().contains(deleter)) {
			throw new IllegalArgumentException("invalid person deleting group");
		}
		
	}

	public void addMember(Group group, Person person) {
		Person adder = getLoggedInUser(identitySpider);
		if (!group.getMembers().contains(adder)) {
			throw new IllegalArgumentException("invalid person adding member to group");
		}		
		group.addMember(person);
	}

	public List<Group> findGroups() {
		return findGroups(getLoggedInUser(identitySpider));
	}

	public List<Group> findGroups(Person viewpoint) {
		Query q;
		q = em.createQuery("from Group g where :personid in elements(g.members)");
		q.setParameter("personid", viewpoint);
		return (List<Group>) q.getResultList();
	}
}
