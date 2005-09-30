package com.dumbhippo.server;

import junit.framework.TestCase;

public class XMLBuilderTest extends TestCase {

	public void testAppendEscaped() {
		XMLBuilder builder = new XMLBuilder();
		
		builder.appendEscaped("&&&");
		assertEquals(builder.toString(), "&amp;&amp;&amp;");
		builder.getStringBuilder().append("foo");
		assertEquals(builder.toString(), "&amp;&amp;&amp;foo");
		builder.getStringBuilder().setLength(0);
		assertEquals(builder.toString(), "");
		builder.appendEscaped("\"'&<>");
		assertEquals(builder.toString(), "&quot;&apos;&amp;&lt;&gt;");
	}

}
