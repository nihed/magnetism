package com.dumbhippo.hungry.readonly;

import java.util.Set;

import com.dumbhippo.hungry.util.CheatSheet;

import junit.framework.TestCase;

public class ViewPersonAll extends TestCase {
	public void testAllPages() {
		CheatSheet cs = CheatSheet.getReadOnly();
		Set<String> userIds = cs.getAllUserIds();
		
		System.out.println("Going over all " + userIds.size() + " users");
		int count = 0;
		for (String id : userIds) {
			System.out.println("   User " + count + " viewperson?personId=" + id);
			ViewPerson vp = new ViewPerson(id);
			vp.setUp();
			vp.testPage();
			++count;
		}
	}
}

