package com.dumbhippo;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("General tests");
        suite.addTest(new TestSuite(TypeFilteredCollectionTest.class));
        suite.addTest(new TestSuite(FilesystemTests.class));
        suite.addTest(new TestSuite(FullNameTest.class));
        suite.addTest(new TestSuite(StringUtilsTest.class));
        suite.addTest(new TestSuite(XmlBuilderTest.class));
        return suite;
    }
}
