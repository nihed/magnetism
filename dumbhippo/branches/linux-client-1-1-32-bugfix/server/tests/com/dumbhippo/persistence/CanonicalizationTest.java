package com.dumbhippo.persistence;

import junit.framework.TestCase;

public class CanonicalizationTest extends TestCase {

	public void testAimCanonicalization() {
		try {
			assertTrue(AimResource.canonicalize("foo bar").equals("foobar"));
			assertTrue(AimResource.canonicalize(" foobar ").equals("foobar"));
		} catch (ValidationException e) {
			throw new RuntimeException("Unexpected invalid aim address", e);
		}
	}
	
	public void testEmailCanonicalization() {
		try {
			// uppercase
			assertTrue(EmailResource.canonicalize("foo@BAR.COM").equals("foo@bar.com"));
			// leading/trailing space
			assertTrue(EmailResource.canonicalize(" foo@bar.com ").equals("foo@bar.com"));
			// no change
			assertTrue(EmailResource.canonicalize("foo@bar.com").equals("foo@bar.com"));
			// another example
			assertTrue(EmailResource.canonicalize("hp@redhat.com").equals("hp@redhat.com"));			
		} catch (ValidationException e) {
			throw new RuntimeException("Unexpected invalid email address", e);
		}
	}
}
