package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

/**
 * Test basic object-management functions of the DMCache 
 * 
 * @author otaylor
 */
public class BasicTests  extends AbstractSupportedTests {
	public void testCreate() throws NotFoundException {
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
		groupDMO = session.find(TestGroupDMO.class, guid);
		assertTrue(groupDMO != null);
		assertTrue(groupDMO.getKey().equals(guid));
		assertTrue(groupDMO.getName().equals("Hippos"));
		
		em.getTransaction().commit();
	}
}
