package com.dumbhippo.hungry.util;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.WebTester;

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
	}
	
	protected PageTestCase() {
		this(new WebTester());
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
	
	@Override
	public void setUp() {
		t.getTestContext().setBaseUrl(getBaseUrl());
	}
	
	/**
	 * Should browse to the page, call validatePage(),
	 * then do any other tests we can think of. Intended
	 * to be invoked when we are the TestCase but not 
	 * when another test case is using us.
	 */
	public abstract void testPage();
}

