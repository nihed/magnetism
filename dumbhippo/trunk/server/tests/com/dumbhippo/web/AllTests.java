package com.dumbhippo.web;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.identity20.GuidTest;

public class AllTests {
	
    public static Test suite() {
        TestSuite suite = new TestSuite("Web tests");
        suite.addTest(new TestSuite(LoginCookieTest.class));
        return suite;
    }
}
