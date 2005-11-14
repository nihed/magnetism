package com.dumbhippo.hungry.destructive;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.PackageSuite;
import com.dumbhippo.hungry.util.WebServices;

public class DestructiveTests {
    public static Test suite() {
    	CheatSheet cs = CheatSheet.getReadWrite();
    	System.out.println("Erasing database contents...");
    	cs.nukeDatabase();
    	System.out.println("Done.");
  
    	// bootstrap at least one user
    	WebServices ws = new WebServices();
    	String userId = ws.getTextPOST("/dologin",
    			"email", "bootstrap@example.com");
    	System.out.println("Bootstrap user is " + userId);
    	
        TestSuite suite = new PackageSuite("Destructive tests", DestructiveTests.class);
        return suite;
    }
}
