package com.dumbhippo.server;

import com.dumbhippo.server.util.HtmlTextExtractor;

import junit.framework.TestCase;

public class TestHtmlTextExtractor extends TestCase {
	
	public void testHtmlTextExtraction() {
		String[] testStrings = { 
				" <p>foo</p><p>bar</p>baz", "foo\n\nbar\n\nbaz",
				"foo<p>bar<p>baz", "foo\n\nbar\n\nbaz",
				" <b> Some  text</b>", "Some text",
				"&amp;&lt;&apos;&nbsp;", "&<'\u00a0",
				"<![CDATA[&&&]]>", "&&&",
				"foo<!-- Comment text -->foo", "foofoo"
			};
		boolean someFailed = false;
		
		for (int i = 0; i < testStrings.length; i += 2) {
			String test = testStrings[i];
			String expected = testStrings[i + 1];
			String result = HtmlTextExtractor.extractText(test);
			
			if (!result.equals(expected)) {
				System.out.println("Test " + (1 + (i / 2)) + " failed");
				System.out.println("Input:\n" + test + "\nExpected:\n" + expected + "\nGot:\n" + result);
				someFailed = true;
			}
		}
		
		assertTrue(!someFailed);
	}
}
