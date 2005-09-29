package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.identity20.RandomToken;

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
