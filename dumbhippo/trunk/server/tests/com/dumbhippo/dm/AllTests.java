package com.dumbhippo.dm;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("Data Model tests");
        
        suite.addTest(new TestSuite(FetchParserTests.class));
        
        suite.addTest(new TestSuite(FilterParserTests.class));
        suite.addTest(new TestSuite(FilterAssemblerTests.class));
        suite.addTest(new TestSuite(FilterStateMapTests.class));
        suite.addTest(new TestSuite(FilterCompilerTests.class));

        suite.addTest(new TestSuite(PersistenceTests.class));
        
        suite.addTest(new TestSuite(BasicTests.class));
        
        suite.addTest(new TestSuite(FetchBindingTests.class));
        suite.addTest(new TestSuite(FetchTests.class));
        suite.addTest(new TestSuite(ThreadingTests.class));
        suite.addTest(new TestSuite(FilterTests.class));
        
        return suite;
    }
}
