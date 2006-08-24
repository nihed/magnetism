package com.dumbhippo.identity20;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
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
		Guid fromString = null;
		try {
			fromString = new Guid(s);
		} catch (ParseException e) {
			// we'll fail below if this happens
		}

		assertTrue(guid.equals(fromString));

		boolean gotException = false;
		try {
			fromString = new Guid("Not a valid guid");
		} catch (ParseException e) {
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
		Guid copy = null;
		try {
			copy = new Guid(guid.toString());
		} catch (ParseException e) {
			// copy will be null and the below will fail
		}
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
			assertEquals(g.toString().length(), Guid.STRING_LENGTH);
			guids.add(g);
		}
		TestUtils.testEqualsImplementation(guids);
	}

	public void testValidate() {
		for (int i = 0; i < 20; ++i) {
			Guid g = Guid.createNew();
			try {
				Guid.validate(g.toString());
			} catch (ParseException e) {
				throw new Error("just-created Guid did not validate", e);
			}
		}
		boolean gotException = false;
		try {
			Guid.validate("notvalid");
		} catch (ParseException e) {
			gotException = true;
		}
		assertTrue(gotException);
	}
}
