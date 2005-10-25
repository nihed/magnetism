/**
 * 
 */
package com.dumbhippo;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Basic braindead simple XML utility class, since Java doesn't come
 * with one.
 * 
 * @author hp
 *
 */
public class XmlBuilder {

	private StringBuilder builder;
	private List<String> elementStack;
	private boolean currentOpenFinished;
	
	public XmlBuilder() {
		builder = new StringBuilder();
		elementStack = new ArrayList<String>();
		currentOpenFinished = true;
	}
	
	public StringBuilder getStringBuilder() {
		return builder;
	}
	
	public void appendStandaloneFragmentHeader() {
		append("<?xml version=\"1.0\" ?>");
	}

	public void appendHtmlHead(String title) {
		// this isn't actually "xml" but whatever
		append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"
		+ "<html>\n"
		+ "<head>\n"
		+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
		+ "<title>");
		appendEscaped(title);
		append("</title>\n</head>\n");
	}
	
	public void appendEscaped(String text) {
		for (char c : text.toCharArray()){
			if (c == '&')
				append("&amp;");
			else if (c == '<')
				append("&lt;");
			else if (c == '>')
				append("&gt;");
			else if (c == '\'')
				append("&apos;");
			else if (c == '"')
				append("&quot;");
			else
				append(c);
		}
	}
	
	/**
	 * Add new open element to stream; must be paired with a closeElement()
	 * 
	 * @param elementName name of element to append
	 * @param attributes list of key value pair
	 */
	public void openElement(String elementName, String... attributes) {
		append("<");
		append(elementName);
		elementStack.add(0, elementName);
		
		if ((attributes.length % 2) != 0) {
			throw new IllegalArgumentException("attributes argument must have even number of elements, since it's name-value pairs");
		}
		
		for (int i = 0; i < attributes.length; i += 2) {
			append(" ");
			append(attributes[i]);
			append("=\"");
			appendEscaped(attributes[i+1]);
			append("\"");	
		}
		currentOpenFinished = false;
	}
	
	/**
	 * Close out most recently created element.
	 */
	public void closeElement() {
		assert elementStack.size() > 0;
		String current = elementStack.remove(0);		
		if (!currentOpenFinished) {
			builder.append("/>");
			currentOpenFinished = true;
		} else {
			append("</");
			append(current);
			append(">");
		}
	}
	
	/**
	 * Appends an element with only text as element child.
	 * 
	 * @param elementName name of element
	 * @param content content inside element or null
	 * @param attributes list of key value pair of attributes
	 */
	public void appendTextNode(String elementName, String content, String... attributes) {
		openElement(elementName, attributes);
		if (content != null)
			appendEscaped(content);
		closeElement();
	}
	
	private void preAppend() {
		if (!currentOpenFinished) {
			builder.append(">");
			currentOpenFinished = true;
		}		
	}
	
	public void append(String s) {
		preAppend();
		builder.append(s);
	}
	
	public void append(char c) {
		preAppend();
		builder.append(c);
	}
	
	public String toString() {
		for (int i = 0; i < elementStack.size(); i++) {
			closeElement();
		}
		return builder.toString();
	}
	
	public static String escape(String text) {
		XmlBuilder builder = new XmlBuilder();
		builder.appendEscaped(text);
		return builder.toString();
	}
}
