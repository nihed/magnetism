package com.dumbhippo;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

	/*
	 * Test method for 'com.dumbhippo.StringUtils.hexEncode(byte[])'
	 */
	public void testHexEncode1() {
		String out = StringUtils.hexEncode(new byte[0]);
		assertNotNull(out);
		assertEquals(out.length(), 0);
	}
	
	public void testHexEncode2() {
		byte[] b = {0x41};
		String out = StringUtils.hexEncode(b);
		assertNotNull(out);
		assertEquals(2, out.length());
		assertEquals(out, "41");
	}
	
	public void testHexEncode3() {
		byte[] b = {0x7F, 0x00, 0x52};
		String out = StringUtils.hexEncode(b);
		assertNotNull(out);
		assertEquals(out.length(), 6);
		assertEquals(out, "7f0052");
	}	
}
