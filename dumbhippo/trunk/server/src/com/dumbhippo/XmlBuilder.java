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
public class XmlBuilder {

	private StringBuilder builder;
	
	public XmlBuilder() {
		builder = new StringBuilder();
	}
	
	public StringBuilder getStringBuilder() {
		return builder;
	}
	
	public void appendStandaloneFragmentHeader() {
		builder.append("<?xml version=\"1.0\" ?>");
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
	
	/**
	 * Appends an element with only text as element child.
	 * 
	 * @param elementName name of element
	 * @param content content inside element or null
	 * @param attributes alternating args name,value,name,value
	 */
	public void appendElement(String elementName, String content, String... attributes) {
		builder.append("<");
		builder.append(elementName);
		
		if ((attributes.length % 2) != 0) {
			throw new IllegalArgumentException("attributes argument must have even number of elements, since it's name-value pairs");
		}
		
		for (int i = 0; i < attributes.length; i += 2) {
			builder.append(" ");
			builder.append(attributes[i]);
			builder.append("=\"");
			appendEscaped(attributes[i+1]);
			builder.append("\"");	
		}
		
		if (content == null || content.length() == 0) {
			builder.append("/>");
		} else {
			builder.append(">");
			appendEscaped(content);
			builder.append("</");
			builder.append(elementName);
			builder.append(">");
		}
	}
	
	public void append(String s) {
		builder.append(s);
	}
	
	public String toString() {
		return builder.toString();
	}
	
	public void setLength(int length) {
		builder.setLength(length);
	}
	
	public static String escape(String text) {
		XmlBuilder builder = new XmlBuilder();
		builder.appendEscaped(text);
		return builder.toString();
	}
}
