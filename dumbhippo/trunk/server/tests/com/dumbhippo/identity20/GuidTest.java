package com.dumbhippo.identity20;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.TestUtils;

import junit.framework.TestCase;

public class GuidTest extends TestCase {

	public void testCreateNew() {
		Guid guid = Guid.createNew();
		assertNotNull(guid);
	}

	/*
	 * Class under test for void Guid(Guid)
	 */
	public void testGuidGuid() {
		Guid guid = Guid.createNew();
		Guid copy = new Guid(guid);
		assertTrue(guid.equals(copy));
		assertTrue(copy.equals(guid));
		guid.toString().equals(copy.toString());
	}

	/*
	 * Class under test for void Guid(String)
	 */
	public void testGuidString() {
		Guid guid = Guid.createNew();
		String s = guid.toString();
		Guid fromString = new Guid(s);

		assertTrue(guid.equals(fromString));

		boolean gotException = false;
		try {
			fromString = new Guid("Not a valid guid");
		} catch (IllegalArgumentException e) {
			gotException = true;
		} finally {
			assertTrue(gotException);
		}
	}

	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
		Guid guid = Guid.createNew();
		Guid copy = new Guid(guid.toString());
		assertTrue(guid.equals(copy));
		assertEquals(guid.toString().length(), Guid.STRING_LENGTH);
	}

	/*
	 * Class under test for boolean equals(Object)
	 */
	public void testEqualsObject() {
		List<Guid> guids = new ArrayList<Guid>();
		for (int i = 0; i < 20; ++i) {
			Guid g = Guid.createNew();
			System.err.println(g.toString());
			assertEquals(g.toString().length(), Guid.STRING_LENGTH);
			guids.add(g);
		}
		TestUtils.testEqualsImplementation(guids);
	}

}
