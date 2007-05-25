package com.dumbhippo.dm;

import java.util.Collections;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public class FilterTests extends AbstractSupportedTests {
	public void createData(Guid bobId, Guid janeId, Guid victorId, 
			               Guid bobAndJaneId, Guid bobOnlyId, Guid janeOnlyId) 
	{ 
		EntityManager em = support.beginTransaction();

		TestUser bob = new TestUser("Bob");
		bob.setId(bobId.toString());
		em.persist(bob);
		
		TestUser jane = new TestUser("Jane");
		jane.setId(janeId.toString());
		em.persist(jane);

		TestUser victor = new TestUser("Victor");
		victor.setId(victorId.toString());
		em.persist(jane);

		TestGroup bobAndJane = new TestGroup("BobAndJane");
		bobAndJane.setId(bobAndJaneId.toString());
		em.persist(bobAndJane);
		
		TestGroupMember groupMember;
		
		groupMember = new TestGroupMember(bobAndJane, bob);
		em.persist(groupMember);
		bobAndJane.getMembers().add(groupMember);

		groupMember = new TestGroupMember(bobAndJane, jane);
		em.persist(groupMember);
		bobAndJane.getMembers().add(groupMember);

		
		TestGroup bobOnly = new TestGroup("BobOnly");
		bobOnly.setId(bobOnlyId.toString());
		bobOnly.setSecret(true);
		em.persist(bobOnly);
		
		groupMember = new TestGroupMember(bobOnly, bob);
		em.persist(groupMember);
		bobOnly.getMembers().add(groupMember);

		
		TestGroup janeOnly = new TestGroup("JaneOnly");
		janeOnly.setId(janeOnlyId.toString());
		em.persist(janeOnly);
		
		groupMember = new TestGroupMember(janeOnly, jane);
		em.persist(groupMember);
		janeOnly.getMembers().add(groupMember);

		em.getTransaction().commit();
	}
	
	public void testFilterFind() throws NotFoundException {
		EntityManager em;
		ReadOnlySession session;
		
		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid victorId = Guid.createNew();
		Guid bobAndJaneId = Guid.createNew();
		Guid bobOnlyId = Guid.createNew();
		Guid janeOnlyId = Guid.createNew();
		
		createData(bobId, janeId, victorId, bobAndJaneId, bobOnlyId, janeOnlyId);

		/////////////////////////////////////////////////
		
		em = support.beginSessionRO(new TestViewpoint(bobId));
		session = ReadOnlySession.getCurrent();

		// Bob can see both all three groups
		session.find(TestGroupDMO.class, bobAndJaneId); 
		session.find(TestGroupDMO.class, bobOnlyId);
		session.find(TestGroupDMO.class, janeOnlyId);

		em.getTransaction().commit();
		
		/////////////////////////////////////////////////
		
		em = support.beginSessionRO(new TestViewpoint(janeId));
		session = ReadOnlySession.getCurrent();

		// But Jane can't see bobOnlyId. because it's secret and 
		// she's not in it
		session.find(TestGroupDMO.class, bobAndJaneId); 
		session.find(TestGroupDMO.class, janeOnlyId);

		try {
			session.find(TestGroupDMO.class, bobOnlyId);
			throw new RuntimeException("BobOnly should not have been visible");
		} catch (NotFoundException e) {
		}

		em.getTransaction().commit();
	}
	
	public void testFilterList() throws NotFoundException {
		EntityManager em;
		ReadOnlySession session;
		
		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid victorId = Guid.createNew();
		Guid bobAndJaneId = Guid.createNew();
		Guid bobOnlyId = Guid.createNew();
		Guid janeOnlyId = Guid.createNew();
		
		createData(bobId, janeId, victorId, bobAndJaneId, bobOnlyId, janeOnlyId);

		/////////////////////////////////////////////////
		
		em = support.beginSessionRO(new TestViewpoint(bobId));
		session = ReadOnlySession.getCurrent();

		// Bob can see both of Jane's groups
		assertEquals(2, session.find(TestUserDMO.class, janeId).getGroups().size()); 

		em.getTransaction().commit();
		
		/////////////////////////////////////////////////
		
		em = support.beginSessionRO(new TestViewpoint(janeId));
		session = ReadOnlySession.getCurrent();

		// Jane only sees Bob's public group
		assertEquals(1, session.find(TestUserDMO.class, bobId).getGroups().size());
		
		em.getTransaction().commit();
		
		/////////////////////////////////////////////////
		
		// If we mark Victor as Jane's enemy, then he can't see Jane's groups at all
		TestViewpoint victorViewpoint = new TestViewpoint(victorId, null, Collections.singletonList(janeId));
		
		em = support.beginSessionRO(victorViewpoint);
		session = ReadOnlySession.getCurrent();

		assertEquals(0, session.find(TestUserDMO.class, janeId).getGroups().size());
		
		em.getTransaction().commit();
	}
}
