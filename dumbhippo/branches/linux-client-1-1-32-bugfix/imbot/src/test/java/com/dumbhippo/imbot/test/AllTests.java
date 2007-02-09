package com.dumbhippo.imbot.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("All tests");
        suite.addTest(new TestSuite(com.dumbhippo.aim.ScreenNameTest.class));
        return suite;
    }
}
