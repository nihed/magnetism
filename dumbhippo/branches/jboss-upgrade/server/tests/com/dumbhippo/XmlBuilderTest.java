package com.dumbhippo;

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
		assertEquals(builder.toString(), "&quot;&#39;&amp;&lt;&gt;");
	}

	public void testAppendElement() {
		XmlBuilder builder = new XmlBuilder();
		
		builder.appendTextNode("elemname", "some > content", "attr1", "val1&<", "attr2", "val2", "hasnullvalue", null);
		
		assertEquals(builder.toString(), "<elemname attr1=\"val1&amp;&lt;\" attr2=\"val2\">some &gt; content</elemname>");

		builder = new XmlBuilder();
		builder.appendTextNode("elemname", null);
		assertEquals(builder.toString(), "<elemname/>");
	}
	
	public void testAppendNested() {
		XmlBuilder builder = new XmlBuilder();
		
		builder.openElement("foo", "baz", "\"=whee");
		builder.appendEscaped("moo");
		builder.openElement("bar", "attr", "&cow");
		builder.closeElement();
		builder.closeElement();
		assertEquals(builder.toString(), "<foo baz=\"&quot;=whee\">moo<bar attr=\"&amp;cow\"/></foo>");		
	}
}
