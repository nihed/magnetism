package com.dumbhippo.hungry.readonly;

import java.util.Set;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.OrderAfter;

import junit.framework.TestCase;

@OrderAfter(Home.class)
public class HomeAll extends TestCase {
	public void testAllPages() {
		CheatSheet cs = CheatSheet.getReadOnly();
		Set<String> userIds = cs.getAllUserIds();
		
		System.out.println("Going over home for all " + userIds.size() + " users");
		int count = 0;
		for (String id : userIds) {
			System.out.println("   User " + count + " id=" + id);
			Home home = new Home(null, id);
			home.setUp();
			home.testPage();
			++count;
		}
	}
}
