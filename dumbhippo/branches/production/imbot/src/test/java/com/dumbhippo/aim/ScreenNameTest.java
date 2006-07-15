package com.dumbhippo.aim;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.dumbhippo.imbot.test.TestUtils;

public class ScreenNameTest extends TestCase {

	public void testScreenName() {
		List<ScreenName> testNames = new ArrayList<ScreenName>();
		
		testNames.add(new ScreenName("Dumb Hippo Bot"));
		testNames.add(new ScreenName("Dumb HippoBot"));
		testNames.add(new ScreenName("dumbhippo bot"));
		testNames.add(new ScreenName("Dumb1000"));
		testNames.add(new ScreenName("Dumber1000"));
		testNames.add(new ScreenName("dumb1000"));
		
		TestUtils.testEqualsImplementation(testNames);
	}
}
