package com.dumbhippo.hungry.readonly;

import com.dumbhippo.hungry.util.PackageSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ReadonlyTests {
    public static Test suite() {
        TestSuite suite = new PackageSuite("Read-only tests", ReadonlyTests.class);
        return suite;
    }
}

