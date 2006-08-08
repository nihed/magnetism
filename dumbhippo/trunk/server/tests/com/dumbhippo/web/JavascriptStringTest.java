package com.dumbhippo.web;

import com.dumbhippo.web.tags.JavascriptStringTag;

import junit.framework.TestCase;

public class JavascriptStringTest extends TestCase {
	
	private String safeStringDump(String input) {
		StringBuilder sb = new StringBuilder();
		sb.append("'");
		for (char c : input.toCharArray()) {
			if (c >= 33 && c <= 126) {
				sb.append(c);
				sb.append(' ');
			} else {
				sb.append(String.format("%04x ", c));
			}
		}
		if (sb.length() > 1)
			sb.setLength(sb.length() - 1);
		sb.append("'");
		return sb.toString();
	}
	
	public void testStringEscaping() {
		final String[] sources = { 
				"abc",
				"'",
				"\"",
				"\r",
				"\n",
				"\f",
				"\t",
				"\\",
				"&",
				">",
				"<",
				"\0",
				"\u007f",  // DEL
				"\u000b", // vertical tab
				"\u00a9", // copyright sign
				"\u00b9"  // superscript 1
			};
		final String[] expected = {
				"'abc'",
				"'\\''",
				"'\\\"'",
				"'\\r'",
				"'\\n'",
				"'\\u000C'",
				"'\\t'",
				"'\\\\'",
				"'\\u0026'",
				"'\\u003E'",
				"'\\u003C'",
				"'\\0'",
				"'\\u007F'", // DEL
				"'\\u000B'", // vertical tab
				"'\\u00A9'", // copyright sign
				"'\\u00B9'"  // superscript 1
		};
		if (sources.length != expected.length)
			throw new RuntimeException("sources and expected have to be the same length");
		StringBuilder allSources = new StringBuilder();
		StringBuilder allExpected = new StringBuilder();
		allExpected.append('\'');
		for (int i = 0; i < sources.length; ++i) {
			String escaped = JavascriptStringTag.toJavascriptString(sources[i]);
			if (!escaped.equals(expected[i])) {
				String err = "Got " + safeStringDump(escaped) + " expected " + safeStringDump(expected[i]) + " (expected readable " + expected[i] + ")";
				System.err.println(err);
				throw new RuntimeException(err);
			}
			//System.out.println(escaped);
			allSources.append(sources[i]);
			// have to chop the quotes here
			allExpected.append(expected[i].substring(1, expected[i].length() - 1));
		}
		allExpected.append('\'');

		// check that the escaper also works on a long string
		String allEscapedStr = JavascriptStringTag.toJavascriptString(allSources.toString());
		String allExpectedStr = allExpected.toString();
		if (!allEscapedStr.equals(allExpectedStr)) {
			String err = "Got " + safeStringDump(allEscapedStr) + " expected " + safeStringDump(allExpectedStr) + " (expected readable " + allExpectedStr + ")";
			System.err.println(err);
			throw new RuntimeException(err);
		}
		// System.out.println(allEscapedStr);
	}
}
