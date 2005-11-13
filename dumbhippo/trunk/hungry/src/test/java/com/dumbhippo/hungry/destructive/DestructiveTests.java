package com.dumbhippo.hungry.destructive;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.hungry.util.PackageSuite;

public class DestructiveTests {
    public static Test suite() {
        TestSuite suite = new PackageSuite("Destructive tests", DestructiveTests.class);
        return suite;
    }
}
