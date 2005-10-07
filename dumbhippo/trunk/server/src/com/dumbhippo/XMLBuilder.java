/**
 * 
 */
package com.dumbhippo;

/**
 * 
 * Basic utility function that is inexplicably not
 * in any library.
 * 
 * @author hp
 *
 */
public class XMLBuilder {

	private StringBuilder builder;
	
	public XMLBuilder() {
		builder = new StringBuilder();
	}
	
	public StringBuilder getStringBuilder() {
		return builder;
	}
	
	public void appendEscaped(String text) {
		for (char c : text.toCharArray()){
			if (c == '&')
				builder.append("&amp;");
			else if (c == '<')
				builder.append("&lt;");
			else if (c == '>')
				builder.append("&gt;");
			else if (c == '\'')
				builder.append("&apos;");
			else if (c == '"')
				builder.append("&quot;");
			else
				builder.append(c);
		}
	}
	
	public String toString() {
		return builder.toString();
	}
	
	public static String escape(String text) {
		XMLBuilder builder = new XMLBuilder();
		builder.appendEscaped(text);
		return builder.toString();
	}
}
