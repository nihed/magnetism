package com.dumbhippo.hungry.example;

import com.dumbhippo.hungry.util.PackageSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExampleTests {
    public static Test suite() {
        TestSuite suite = new PackageSuite("Example tests", ExampleTests.class);
        return suite;
    }
}

