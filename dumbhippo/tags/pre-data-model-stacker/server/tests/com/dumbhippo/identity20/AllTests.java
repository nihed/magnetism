package com.dumbhippo.identity20;

import com.dumbhippo.identity20.GuidTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("Identity 2.0 tests");
        suite.addTest(new TestSuite(GuidTest.class));
        suite.addTest(new TestSuite(RandomTokenTest.class));
        return suite;
    }
}
