package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestSuperUserDMO;
import com.dumbhippo.dm.persistence.TestSuperUser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

public class InheritanceTests  extends AbstractSupportedTests {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(InheritanceTests.class);

	public void testInheritance() throws NotFoundException {
		TestSuperUser superUser;
		TestSuperUserDMO superUserDMO;
		EntityManager em;
		ReadOnlySession session;
		Guid guid;
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		em = support.beginSessionRW(viewpoint);

		superUser = new TestSuperUser("The Nose", "The ability to tell if leftovers have gone bad");
		guid = superUser.getGuid();
		em.persist(superUser);

		em.getTransaction().commit();
		
		//////////////////////////////////////

		em = support.beginSessionRO(viewpoint);
		
		session = support.currentSessionRO();
		
		superUserDMO = session.find(TestSuperUserDMO.class, guid);
		assertTrue(superUserDMO != null);
		assertEquals(guid.toString(), superUserDMO.getKey().toString());
		assertEquals("http://mugshot.org/o/test/user/" + guid, superUserDMO.getResourceId());
		assertEquals("*The Nose*", superUserDMO.getName());
		assertEquals("The ability to tell if leftovers have gone bad", superUserDMO.getSuperPower());

		/// Test that groups in base classes are handled in inherited classes
		assertEquals("initializedA", superUserDMO.getGroupedA());
		assertEquals("initializedB", superUserDMO.getGroupedB());

		em.getTransaction().commit();
	}
}
