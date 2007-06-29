package com.dumbhippo.web;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
    public static Test suite() {
        TestSuite suite = new TestSuite("Web tests");
        suite.addTest(new TestSuite(LoginCookieTest.class));
        suite.addTest(new TestSuite(JavascriptStringTest.class));
        return suite;
    }
}
