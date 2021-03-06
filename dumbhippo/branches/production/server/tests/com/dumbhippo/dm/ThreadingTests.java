package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.test.util.Sequence;
import com.dumbhippo.test.util.Sequencer;

public class ThreadingTests extends AbstractSupportedTests {
	@SuppressWarnings("unused")
	private static Logger logger = GlobalSetup.getLogger(ThreadingTests.class);

	public void testStaleData() {
		final Guid viewerId = Guid.createNew();
		final Guid groupId = Guid.createNew();
		
		Sequencer.run2(new Sequence() {
			@Override
			public void run() {
				TestViewpoint viewpoint = new TestViewpoint(viewerId);
				EntityManager em;
				TestGroup group;
				ReadWriteSession session;
				
				/**********/ step(1); /**********/
				
				// Create a new object
				
				em = support.beginSessionRW(viewpoint);

				group = new TestGroup("Hippos");
				group.setId(groupId.toString());
				em.persist(group);

				em.getTransaction().commit();
				
				/**********/ step(3); /**********/
				
				// Modify the object's properties
				
				em = support.beginSessionRW(viewpoint);
				session = support.currentSessionRW();

				group = em.find(TestGroup.class, groupId.toString());
				group.setName("Aardvarks");
				session.changed(TestGroupDMO.class, groupId, "name");
				
				em.getTransaction().commit();
			}
		},new Sequence() {
			@Override
			public void run() throws Exception {
				TestViewpoint viewpoint = new TestViewpoint(viewerId);
				EntityManager em;
				TestGroup group;
				TestGroupDMO groupDMO;
				ReadOnlySession session;
				
				/**********/ step(2); /**********/
				
				// Load the object into a new Hibernate session
				
				em = support.beginSessionRO(viewpoint);
				session = support.currentSessionRO();
								
				group = em.find(TestGroup.class, groupId.toString());
				assertEquals("Hippos", group.getName());
				
				/**********/ step(4); /**********/
				
				// Now get the DMO, which, ignoring the change that we just made and
				// committed to the object,  would normally cause the property values
				// read in step 2 to get written to the global cache
				
				groupDMO = session.find(TestGroupDMO.class, groupId);
				assertEquals("Hippos", groupDMO.getName());
				
				em.getTransaction().commit();
				
				// In a new transaction, we should now see the new values, not the
				// values we read in the last transaction
				
				em = support.beginSessionRO(viewpoint);
				session = support.currentSessionRO();
				
				groupDMO = session.find(TestGroupDMO.class, groupId);
				assertEquals("Aardvarks", groupDMO.getName());

				em.getTransaction().commit();
			}
		});
	}
}
