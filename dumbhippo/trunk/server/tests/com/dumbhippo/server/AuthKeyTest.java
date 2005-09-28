package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class AuthKeyTest extends TestCase {

	public void testAuthKey() {
		List<AuthKey> keys = new ArrayList<AuthKey>();
		for (int i = 0; i < 20; ++i) {
			AuthKey k = AuthKey.createNew();
			keys.add(k);
		}
		TestUtils.testEqualsImplementation(keys);
	}

}
