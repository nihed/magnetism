package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;

public class FetchTests extends AbstractFetchTests {
	static private final Logger logger = GlobalSetup.getLogger(BasicTests.class);
	
	public FetchTests() {
		super("fetch-tests.xml");
	}
	
	private void createData(Guid bobId, Guid janeId, Guid groupId) {
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		em = support.beginTransaction();

		TestUser bob = new TestUser("Bob");
		bob.setId(bobId.toString());
		em.persist(bob);
		
		TestUser jane = new TestUser("Jane");
		jane.setId(janeId.toString());
		em.persist(jane);

		TestGroup group = new TestGroup("BobAndJane");
		group.setId(groupId.toString());
		em.persist(group);
		
		TestGroupMember groupMember;
		
		groupMember = new TestGroupMember(group, bob);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		groupMember = new TestGroupMember(group, jane);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		em.getTransaction().commit();
	}
	
	// Basic operation of fetching, no subclassing, no defaults
	public void testBasicFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);
		
		TestGroupDMO groupDMO = support.currentSessionRO().find(TestGroupDMO.class, groupId);
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "name;members member name", "bobAndJane",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		em.getTransaction().commit();
	}
	
	// Like testBasicFetch but using defaulted fetches
	public void testDefaultFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);
		
		TestGroupDMO groupDMO = support.currentSessionRO().find(TestGroupDMO.class, groupId);
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "+;members +", "bobAndJaneDefault",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		em.getTransaction().commit();
	}
	
	// Test suppression of already known information for repeated fetches with the same client
	public void testMultipleFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestDMClient client = new TestDMClient(support.getModel(), viewpoint.getViewerId());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(client);
		
		TestGroupDMO groupDMO = support.currentSessionRO().find(TestGroupDMO.class, groupId);
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "name", "bobAndJaneSmall",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "+;members +", "bobAndJaneRemaining",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());

		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "members group", "bobAndJaneAddOn",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());

		em.getTransaction().commit();
	}
	
	// Test a fetch that loops back to the same object
	public void testLoopFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestDMClient client = new TestDMClient(support.getModel(), viewpoint.getViewerId());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(client);
		
		TestGroupDMO groupDMO = support.currentSessionRO().find(TestGroupDMO.class, groupId);
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "+;members group +", "bobAndJaneLoop",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());

		em.getTransaction().commit();
	}
	
	public void testNotificationFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestDMClient client = new TestDMClient(support.getModel(), viewpoint.getViewerId());
		TestGroup group;
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid victorId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(client);
		
		TestGroupDMO groupDMO = support.currentSessionRO().find(TestGroupDMO.class, groupId);
		doFetchTest(Guid.class, TestGroupDMO.class, groupDMO, "name;members member name", "bobAndJane",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		em.getTransaction().commit();

		em = support.beginSessionRW(viewpoint);

		ReadWriteSession session = support.currentSessionRW();
		
		group = em.find(TestGroup.class, groupId.toString());
		
		TestUser victor = new TestUser("Victor");
		victor.setId(victorId.toString());
		em.persist(victor);

		TestGroupMember groupMember;
		
		groupMember = new TestGroupMember(group, victor);
		em.persist(groupMember);
		group.getMembers().add(groupMember);
		session.changed(TestGroupDMO.class, groupId, "members");

		group.setName("BobAndJaneAndVictor");
		session.changed(TestGroupDMO.class, groupId, "name");

		em.getTransaction().commit();
		
		support.getModel().waitForAllNotifications();
		
		FetchResult expected = getExpected("andNowVictor",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString(),
				"victor", victorId.toString());
		assertNotNull(client.getLastNotification());
		
		logger.debug("Notification from addition of Victor is {}", client.getLastNotification());
		client.getLastNotification().validateAgainst(expected);
	}

	// TODO: add tests here for fetching against subclassed objects
}
