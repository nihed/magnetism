package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.PageTestCase;

public class Signin extends PageTestCase {

	public Signin() {
		
	}
	
	public Signin(WebTester t) {
		super(t);
	}
	
	@Override
	public void testPage() {
		t.beginAt("/who-are-you");
		validatePage();
	}
	
	public void validatePage() {
		t.assertFormPresent("dhLoginNoPasswordForm");
		t.assertFormPresent("dhLoginWithPasswordForm");
		t.setWorkingForm("dhLoginNoPasswordForm");		
		t.assertFormElementPresent("sendlink");	
		t.setWorkingForm("dhLoginWithPasswordForm");			
		t.assertFormElementPresent("checkpassword");
	}
}
