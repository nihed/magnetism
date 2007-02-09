package com.dumbhippo.postinfo;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("PostInfo tests");
        suite.addTest(new TestSuite(PostInfoTest.class));
        return suite;
    }
}
