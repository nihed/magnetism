package com.dumbhippo.identity20;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.RandomToken;
import com.dumbhippo.server.TestUtils;

import junit.framework.TestCase;

public class RandomTokenTest extends TestCase {

	public void testAuthKey() {
		List<RandomToken> keys = new ArrayList<RandomToken>();
		for (int i = 0; i < 20; ++i) {
			RandomToken k = RandomToken.createNew();
			keys.add(k);
		}
		TestUtils.testEqualsImplementation(keys);
	}

}
