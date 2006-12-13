package com.dumbhippo.hungry.util;

public interface PageChecker {

	/**
	 * Assuming the current WebTester, run assertions 
	 * about the page that will be valid for this page in 
	 * <i>any context</i>, i.e. any test that navigates to 
	 * this page can validatePage()
	 */
	public void validatePage();
}
