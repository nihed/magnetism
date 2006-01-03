package com.dumbhippo.hungry.util;

import java.util.Arrays;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.WebTester;

import com.meterware.httpunit.HttpUnitOptions;

/**
 * A base class for tests of a web page. Convenience functions can 
 * go in here, but the other idea is that we require some standard
 * test methods that should be used whenever this page occurs in 
 * a web conversation. So another test case could instantiate 
 * this one and call those methods. These methods are in PageChecker.
 * 
 * @author hp
 *
 */
public abstract class PageTestCase extends TestCase implements PageChecker {
	
	protected WebTester t;
	
	protected PageTestCase(WebTester t) {
		super();
		this.t = t;
		if (this.t == null)
			this.t = new WebTester();
		setUserAgent(UserAgent.IE6);
	}
	
	protected PageTestCase() {
		this(null);
	}
	
	public void setUserAgent(UserAgent agent) {
		t.getTestContext().setUserAgent(agent.getText());
	}
	
	/**
	 * Override this to change the base url for this test case.
	 * Normally this will return the default for the whole 
	 * suite, if you don't override it.
	 * 
	 * @return base url for this case
	 */
	public String getBaseUrl() {
		return Config.getDefault().getValue(ConfigValue.BASEURL);
	}
	
	/**
	 * Override this to turn off scripting for the page
	 * @return true if scripting is enabled
	 */
	public boolean getScriptEnabled() {
		return false;
	}
	
	@Override
	public void setUp() {
		t.getTestContext().setBaseUrl(getBaseUrl());
		
		// this is global, but should be OK if we set it each time
		HttpUnitOptions.setScriptingEnabled(getScriptEnabled());
	}
	
	/**
	 * Should browse to the page, call validatePage(),
	 * then do any other tests we can think of. Intended
	 * to be invoked when we are the TestCase but not 
	 * when another test case is using us.
	 */
	public abstract void testPage();
	
	protected String assertSignedIn() {
		// Bug in jwebunit; t.getTestContext.getWebClient() has the right cookies,
		// but t.whateverCookieMethod() does not have them
		//t.assertCookiePresent("auth");
		String auth = t.getTestContext().getWebClient().getCookieValue("auth");
		
		if (auth == null) {
			System.out.println("Cookies:");
			System.out.println(Arrays.toString(t.getTestContext().getWebClient().getCookieNames()));
		}
		assertNotNull(auth);
		
		int i = auth.indexOf("name=");
		i += "name=".length();
		int j = auth.indexOf("&", i);
		return auth.substring(i, j);
	}
}

