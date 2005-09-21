package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dumbhippo.server.Identity;

import junit.framework.TestCase;

public class IdentityTest extends TestCase {

	public List<Identity> createTestIdentities() {
		List<Identity> tests = new ArrayList<Identity>();
		Identity id;
		
		// empty identity (GUID only)
		id = Identity.createNew();
		tests.add(id);
		
		// identity with one email
		id = Identity.createNew();
		id.setPropertyAsString(Identity.Property.EMAILS, "foo@bar.com");
		tests.add(id);
		
		// identity with full name
		id = Identity.createNew();
		id.setProperty(Identity.Property.FULLNAME, "Foo Q. Bar");
		tests.add(id);
		
		// identity with a couple properties
		id = Identity.createNew();
		id.setPropertyAsString(Identity.Property.EMAILS, "baz@foo.com");
		id.setProperty(Identity.Property.FULLNAME, "Dumb The Amazing Hippo");
		tests.add(id);
		
		return tests;
	}
	
	public void testMisc() {
		List<Identity> tests = createTestIdentities();
		
		TestUtils.testEqualsImplementation(tests);
	}

	public void testCreateFromProperties() {
		Map<Identity.Property,Object> props = new HashMap<Identity.Property,Object>();
		
		boolean complainedAboutNoGuid = false;
		try {
			Identity id = Identity.createFromProperties(props);
		} catch (IllegalArgumentException e) {
			complainedAboutNoGuid = true;
		} finally {
			assertTrue (complainedAboutNoGuid);
		}
		
		Identity.Guid guid = Identity.Guid.createNew();
		
		props.put(Identity.Property.GUID, guid);
		
		List<String> emails = new ArrayList<String>();
		emails.add("hippo@dumbness.com");
		
		props.put(Identity.Property.EMAILS, emails);
		
		Identity id = Identity.createFromProperties(props);
		
		List gotEmails = (List) id.getProperty(Identity.Property.EMAILS);
		assertTrue(gotEmails.equals(emails));
		assertTrue(((String)gotEmails.get(0)).equals("hippo@dumbness.com"));
		
		Identity.Guid storedGuid = (Identity.Guid) id.getProperty(Identity.Property.GUID);
		assertTrue(storedGuid.equals(guid));
	}

	public void testHasProperty() {
		List<Identity> tests = createTestIdentities();
		for (Identity id : tests) {
			for (Identity.Property p : id.listProperties()) {
				assertTrue(id.hasProperty(p));
			}
			for (Identity.Property p : Identity.Property.values()) {
				if (id.hasProperty(p)) {
					Object o = id.getProperty(p);
					
					// not guaranteed by interface contract, but no 
					// current property allows a null value
					assertTrue(o != null);
				} else {
					Object o = id.getProperty(p);
					
					assertTrue(o == null);
				}
			}
		}
	}

	public void testGetProperty() {
		List<Identity> tests = createTestIdentities();
		for (Identity id : tests) {
			for (Identity.Property p : id.listProperties()) {
				Object o = id.getProperty(p);
			}
		}
	}

	public void testSetProperty() {
		Identity id = Identity.createNew();
		List<String> emails = new ArrayList<String>();
		emails.add("foo@bar.com");
		emails.add("sfo@dfj.com");
		
		id.setProperty(Identity.Property.EMAILS, emails);
		id.setProperty(Identity.Property.FULLNAME, "John Doe");
	}

	public void testSetPropertyAsString() {
		Identity id = Identity.createNew();
		id.setPropertyAsString(Identity.Property.EMAILS, "hello@bar.com,foo@bar.com");
		id.setPropertyAsString(Identity.Property.FULLNAME, "Foo Bar");
		
		boolean caughtBadGUID = false;
		try {
			id.setPropertyAsString(Identity.Property.GUID, "This is not a valid GUID");
		} catch (IllegalArgumentException e) {
			caughtBadGUID = true;
		} finally {
			assertTrue(caughtBadGUID);
		}
		
		// test a three-hyphen thing without numbers in it
		caughtBadGUID = false;
		try {
			id.setPropertyAsString(Identity.Property.GUID, "another-bad-guid");
		} catch (IllegalArgumentException e) {
			caughtBadGUID = true;
		} finally {
			assertTrue(caughtBadGUID);
		}
	}

	public void testGetPropertyAsString() {
		List<Identity> tests = createTestIdentities();
		for (Identity id : tests) {
			for (Identity.Property p : id.listProperties()) {
				String s = id.getPropertyAsString(p);
				Object orig = id.getProperty(p);
				id.setPropertyAsString(p, s);
				Object parsed = id.getProperty(p);
				
				// System.out.printf("%s: %s\n", p.toString(), s);
				
				if (!orig.equals(parsed)) {
					throw new Error(String.format("Round trip through string failed property %s string rep '%s'",
							p.toString(), s));
				}
			}
		}
	}

}
