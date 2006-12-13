package com.dumbhippo.server;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.identity20.GuidTest;

public class AllTests {
	
    public static Test suite() {
        TestSuite suite = new TestSuite("Server tests");
        suite.addTest(new TestSuite(BasicTests.class));
        suite.addTest(new TestSuite(GuidTest.class));
        suite.addTest(new TestSuite(TestHtmlTextExtractor.class));
        return suite;
    }
}
