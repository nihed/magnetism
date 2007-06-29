package com.dumbhippo.hungry.performance;

public class Home extends TimePage {

	public Home() {
		super("/");  // should be "home" not "main" for a logged in user
	}
	
	public void testSingle() {
		timePage(1, 1);
	}

	public void testMultiple() {
		timePage(1, 10);
	}
	
	public void testParallel3() {
		timePage(3, 10);
	}

	public void testParallel5() {
		timePage(5, 10);
	}
}
