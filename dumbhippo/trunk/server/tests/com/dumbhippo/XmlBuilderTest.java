package com.dumbhippo;

import com.dumbhippo.XmlBuilder;

import junit.framework.TestCase;

public class XmlBuilderTest extends TestCase {

	public void testAppendEscaped() {
		XmlBuilder builder = new XmlBuilder();
		
		builder.appendEscaped("&&&");
		assertEquals(builder.toString(), "&amp;&amp;&amp;");
		builder.getStringBuilder().append("foo");
		assertEquals(builder.toString(), "&amp;&amp;&amp;foo");
		builder.getStringBuilder().setLength(0);
		assertEquals(builder.toString(), "");
		builder.appendEscaped("\"'&<>");
		assertEquals(builder.toString(), "&quot;&apos;&amp;&lt;&gt;");
	}

	public void testAppendElement() {
		XmlBuilder builder = new XmlBuilder();
		
		builder.appendElement("elemname", "some > content", "attr1", "val1&<", "attr2", "val2");
		
		assertEquals(builder.toString(), "<elemname attr1=\"val1&amp;&lt;\" attr2=\"val2\">some &gt; content</elemname>");

		builder.setLength(0);
		
		builder.appendElement("elemname", null);
		assertEquals(builder.toString(), "<elemname/>");
	}
}
