package com.dumbhippo.persistence;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("Persistance tests");
        suite.addTest(new TestSuite(GuidPersistableTest.class));
        return suite;
    }
}
