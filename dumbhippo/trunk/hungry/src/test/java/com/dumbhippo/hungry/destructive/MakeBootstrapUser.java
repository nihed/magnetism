package com.dumbhippo.hungry.destructive;

import com.dumbhippo.hungry.readonly.Signin;

public class MakeBootstrapUser extends Signin {

	@Override
	public void testPage() {
		super.testPage();
		
		t.setFormElement("address", "bootstrap@example.com");
		t.setFormElement("password", "blahblah");
		t.submit("checkpassword");
	}
}
