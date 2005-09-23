package com.dumbhippo.server;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("All tests");
        suite.addTest(new TestSuite(DummyTest.class));
        suite.addTest(new TestSuite(BasicTests.class));
        suite.addTest(new TestSuite(GuidTest.class));
        suite.addTest(new TestSuite(ResourceTest.class));   
        return suite;
    }
}
