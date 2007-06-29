package com.dumbhippo.hungry.destructive;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.PackageSuite;
import com.dumbhippo.hungry.util.Config;
import com.dumbhippo.hungry.util.ConfigValue;

public class DestructiveTests {
    public static Test suite() {
	System.out.println("Destructive test suite, site is " +
			   Config.getDefault().getValue(ConfigValue.BASEURL));
    	CheatSheet cs = CheatSheet.getReadWrite();
    	System.out.println("Erasing database contents...");
    	cs.nukeDatabase();
    	System.out.println("Done.");
    	
        TestSuite suite = new PackageSuite("Destructive tests", DestructiveTests.class);
        return suite;
    }
}
