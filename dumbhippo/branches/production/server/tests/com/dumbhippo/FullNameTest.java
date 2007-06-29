package com.dumbhippo;

import com.dumbhippo.FullName;
import com.dumbhippo.server.TestUtils;

import junit.framework.TestCase;

public class FullNameTest extends TestCase {

	public void testFullName() {
		FullName havoc = new FullName("Robert", "Sanford", "Havoc", "Pennington");
		String havocDbString = "Robert Sanford Havoc Pennington";
		FullName owen = new FullName("Owen", "Taylor");
		FullName jim = new FullName("Jim");
		String jimDbString = "Jim";
		FullName bob = new FullName("Bob");
		FullName doe = new FullName("John", "Quincy", "Doe");
		
		TestUtils.testEqualsImplementation(havoc, owen, jim, bob, doe);
		
		assertEquals(havoc.getFirstName(), "Robert");
		assertEquals(havoc.getMiddleName(), "Sanford Havoc");
		assertEquals(havoc.getLastName(), "Pennington");
		assertEquals(havoc.getFullName(), "Robert Sanford Havoc Pennington");
		assertEquals(havoc.getDatabaseString(), havocDbString);
		assertEquals(owen.getFirstName(), "Owen");
		assertEquals(owen.getMiddleName(), "");
		assertEquals(owen.getLastName(), "Taylor");
		assertEquals(owen.getDatabaseString(), "Owen Taylor");
		assertEquals(owen.getFullName(), "Owen Taylor");
		assertEquals(jim.getFirstName(), "Jim");
		assertEquals(jim.getMiddleName(), "");
		assertEquals(jim.getLastName(), "");
		assertEquals(jim.getDatabaseString(), jimDbString);
		assertEquals(doe.getFirstName(), "John");
		assertEquals(doe.getMiddleName(), "Quincy");
		assertEquals(doe.getLastName(), "Doe");
		assertEquals(doe.getDatabaseString(), "John Quincy Doe");
		
		FullName tmpName;
		tmpName = FullName.parseDatabaseString(havocDbString);
		assertEquals(tmpName, havoc);
		tmpName = FullName.parseDatabaseString(jimDbString);
		assertEquals(tmpName, jim);	
	}
}
