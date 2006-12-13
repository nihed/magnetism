package com.dumbhippo.hungry.performance;

import com.dumbhippo.hungry.util.PackageSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PerformanceTests {
    public static Test suite() {
        TestSuite suite = new PackageSuite("Performance tests", PerformanceTests.class);

        return suite;
    }
}

