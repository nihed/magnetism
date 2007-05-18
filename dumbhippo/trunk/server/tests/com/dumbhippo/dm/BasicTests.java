package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.dm.TestGroupMemberDMO;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

/**
 * Test basic object-management functions of the DMCache 
 * 
 * @author otaylor
 */
public class BasicTests  extends AbstractSupportedTests {
	static private final Logger logger = GlobalSetup.getLogger(BasicTests.class);

	// Test whether properties can be retrieved from the session and global caches
	public void testCaching() throws NotFoundException {
		TestGroup group;
		TestGroupDMO groupDMO;
		EntityManager em;
		ReadOnlySession session;
		Guid guid;
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		em = support.beginSessionRW(viewpoint);

		group = new TestGroup("Hippos");
		guid = group.getGuid();
		em.persist(group);

		em.getTransaction().commit();
		
		//////////////////////////////////////

		em = support.beginSessionRO(viewpoint);
		
		session = ReadOnlySession.getCurrent();
		
		// First time stores in session-local and global caches
		groupDMO = session.find(TestGroupDMO.class, guid);
		assertTrue(groupDMO != null);
		assertTrue(groupDMO.getKey().equals(guid));
		assertTrue(groupDMO.getName().equals("Hippos"));
		
		// Second time within session finds the existing DMO in
		// the session-local cache. The property value is cached
		// in the DMO.
		groupDMO = session.find(TestGroupDMO.class, guid);
		assertTrue(groupDMO != null);
		assertTrue(groupDMO.getKey().equals(guid));
		assertTrue(groupDMO.getName().equals("Hippos"));

		em.getTransaction().commit();
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);

		session = ReadOnlySession.getCurrent();
		
		// Creates a new GroupDMO. The property value will be found
		// from the global cache
		groupDMO = session.find(TestGroupDMO.class, guid);
		assertTrue(groupDMO != null);
		assertTrue(groupDMO.getKey().equals(guid));
		assertTrue(groupDMO.getName().equals("Hippos"));
		
		em.getTransaction().commit();
	}

	// Test multi-valued properties and custom keys
	public void testMultiValued() {
		EntityManager em;

		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		/////////////////////////////////////////////////
		// Setup

		em = support.beginSessionRW(viewpoint);

		TestUser bob = new TestUser("Bob");
		Guid bobId = bob.getGuid();
		em.persist(bob);
		
		TestUser jane = new TestUser("Jane");
		Guid janeId = jane.getGuid();
		em.persist(jane);

		TestGroup group = new TestGroup("BobAndJane");
		Guid groupId = group.getGuid();
		em.persist(group);
		
		TestGroupMember groupMember;
		
		groupMember = new TestGroupMember(group, bob);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		groupMember = new TestGroupMember(group, jane);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		em.getTransaction().commit();

		/////////////////////////////////////////////////

		em = support.beginSessionRO(viewpoint);

		// Check that the group looks OK read as DMO's from a new transaction.
		logger.debug("===== Checking the group in a new transaction ====\n");
		checkGroupValidity(groupId, bobId, janeId);

		// Check again with everything cached in the sesssion 
		logger.debug("===== Checking the group again in the same transaction ====\n");
		checkGroupValidity(groupId, bobId, janeId);

		em.getTransaction().commit();
		
		em = support.beginSessionRO(viewpoint);

		// Now check once again in another new transaction to test reading
		// back from the global cache
		logger.debug("===== Checking the group again in a different new transaction ====\n");
		checkGroupValidity(groupId, bobId, janeId);

		em.getTransaction().commit();
	}

	private void checkGroupValidity(Guid groupId, Guid bobId, Guid janeId) {
		ReadOnlySession session = ReadOnlySession.getCurrent();
		
		TestGroupDMO groupDMO = session.findMustExist(TestGroupDMO.class, groupId);
		assertNotNull(groupDMO);
		assertEquals(groupId, groupDMO.getKey());

		boolean seenBob = false;
		boolean seenJane = false;
		
		assertEquals(2, groupDMO.getMembers().size());
		
		for (TestGroupMemberDMO groupMemberDMO : groupDMO.getMembers()) {
			TestUserDMO memberDMO = groupMemberDMO.getMember();
			if (memberDMO.getKey().equals(bobId)) {
				seenBob = true;
				assertEquals("Bob", memberDMO.getName());
			}
			if (memberDMO.getKey().equals(janeId)) {
				seenJane = true;
				assertEquals("Jane", memberDMO.getName());
			}
		}
		
		assertTrue(seenBob && seenJane);
	}
}
