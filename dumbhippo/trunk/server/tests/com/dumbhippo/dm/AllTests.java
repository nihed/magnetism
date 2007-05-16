package com.dumbhippo.dm;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("Data Model tests");
        suite.addTest(new TestSuite(PersistenceTests.class));
        return suite;
    }
}
