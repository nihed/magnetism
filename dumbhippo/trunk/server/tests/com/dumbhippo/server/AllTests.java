package com.dumbhippo.server;

import com.dumbhippo.identity20.GuidTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
    public static Test suite() {
        TestSuite suite = new TestSuite("Server tests");
        suite.addTest(new TestSuite(BasicTests.class));
        suite.addTest(new TestSuite(GuidTest.class));
        suite.addTest(new TestSuite(EmailResourceTest.class));
        suite.addTest(new TestSuite(IdentitySpiderBeanTest.class)); 
        suite.addTest(new TestSuite(PersonViewTests.class));          
        suite.addTest(new TestSuite(InvitationSystemBeanTest.class));
        return suite;
    }
}
