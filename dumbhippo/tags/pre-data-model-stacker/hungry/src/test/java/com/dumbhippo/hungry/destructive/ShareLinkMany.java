package com.dumbhippo.hungry.destructive;

import java.util.Set;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.OrderAfter;

import junit.framework.TestCase;

@OrderAfter(ShareLinkDestructive.class)
public class ShareLinkMany extends TestCase {
	public void testShareLinkMany() {
		CheatSheet cs = CheatSheet.getReadOnly();
		Set<String> userIds = cs.getSampleUserIds(10);
		for (String userId : userIds) {
			ShareLinkDestructive sld = new ShareLinkDestructive(null, userId);
			sld.setUp();
			sld.testPage();
		}
	}
}
