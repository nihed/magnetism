package com.dumbhippo.hungry.performance;

import com.dumbhippo.hungry.readonly.Signin;
import com.dumbhippo.hungry.util.SkipTest;
import com.dumbhippo.hungry.util.WebServices;

/**
 * "Test page" for creating a user to use for performance data. It derives
 * from the test page infrastructure mainly to reuse some code for
 * authenticating and signin in; it doesn't actually correspond to
 * a single page.
 * 
 * @author otaylor
 */
@SkipTest
public class MakeUser extends Signin {
	public String emailAddress;
	public String name;
	public String userId;
	
	public MakeUser(String emailAddress, String name) {
		this.emailAddress = emailAddress;
		this.name = name;
	}
	
	public String getUserId() {
		return userId;
	}

	@Override
	public void testPage() {
		super.testPage();
		
		t.setFormElement("address", emailAddress);
		t.setFormElement("password", "blahblah");
		t.submit("checkpassword");
		
		userId = assertSignedIn();
		System.out.println("Created user " + userId + " for email " + emailAddress);
		
		WebServices ws = new WebServices(t);
		ws.doPOST("/renameperson",
 				  "name", name);
		
		System.out.println("Set name of " + userId + " to " + name);		
	}
}
