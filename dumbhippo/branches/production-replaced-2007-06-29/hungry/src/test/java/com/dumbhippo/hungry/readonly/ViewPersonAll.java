package com.dumbhippo.hungry.readonly;

import java.util.Set;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.OrderAfter;

import junit.framework.TestCase;

@OrderAfter(ViewPerson.class)
public class ViewPersonAll extends TestCase {
	public void testAllPages() {
		CheatSheet cs = CheatSheet.getReadOnly();
		Set<String> userIds = cs.getAllUserIds();
		
		System.out.println("Going over viewperson for all " + userIds.size() + " users");
		int count = 0;
		for (String id : userIds) {
			System.out.println("   User " + count + " person?who=" + id);
			ViewPerson vp = new ViewPerson(null, id);
			vp.setUp();
			vp.testPage();
			++count;
		}
	}
}

