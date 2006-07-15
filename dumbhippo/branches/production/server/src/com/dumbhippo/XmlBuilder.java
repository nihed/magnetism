/**
 * 
 */
package com.dumbhippo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * Basic braindead simple XML utility class, since Java doesn't come
 * with one.
 * 
 * @author hp
 *
 */
public class XmlBuilder implements Appendable {

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
		append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
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
	
	public void appendEscaped(String text, Collection<String> searchTerms) {
		
		int start = builder.length();
		
		for (char c : text.toCharArray()){
			if (c == '&')
				append("&amp;");
			else if (c == '<')
				append("&lt;");
			else if (c == '>')
				append("&gt;");
			else if (c == '\'')
				append("&#39;"); // &apos; is valid XML but not valid HTML
			else if (c == '"')
				append("&quot;");
			else
				append(c);
		}
		
		int end = builder.length();
		
		if (searchTerms != null && end > start) {
			for (String t : searchTerms) {
				String tHtml = XmlBuilder.escape(t);
				
				String highlighted = builder.substring(start, end).replace(tHtml,
						"<span style=\"font-weight: bold; color: red;\">" + tHtml + "</span>");
				
				builder.replace(start, end, highlighted);
			}
		}
	}
	
	public void appendEscaped(String text) {
		appendEscaped(text, null);
	}
	
	/**
	 * Add new open element to stream; must be paired with a closeElement()
	 * 
	 * @param elementName name of element to append
	 * @param attributes list of key value pairs; if value is null, the pair is ignored
	 */
	public void openElement(String elementName, String... attributes) {
		append("<");
		append(elementName);
		elementStack.add(0, elementName);
		
		if ((attributes.length % 2) != 0) {
			throw new IllegalArgumentException("attributes argument must have even number of elements, since it's name-value pairs");
		}
		
		for (int i = 0; i < attributes.length; i += 2) {
			// ignore attrs with null value
			if (attributes[i+1] != null) {
				append(" ");
				append(attributes[i]);
				append("=\"");
				appendEscaped(attributes[i+1]);
				append("\"");	
			}
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
	 * @param attributes list of key value pair of attributes; if value is null, the pair is ignored
	 */
	public void appendTextNode(String elementName, String content, String... attributes) {
		openElement(elementName, attributes);
		if (content != null)
			appendEscaped(content);
		closeElement();
	}
	

	/**
	 * A convenience function useful when sending html mail or 
	 * showing post on a web page. Converts plain text to 
	 * HTML, adding &lt;p&gt; and &lt;br&gt; tags for example.
	 * 
	 * @param text the text to convert and append
	 */
	public void appendTextAsHtml(String text, Collection<String> searchTerms) {	
		append("<p>\n");
		// I suppose this is a wasteful way to be able to use getLine()
		BufferedReader reader = new BufferedReader(new StringReader(text));
		try {
			boolean lastLineEmpty = false;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.matches("^\\s*$")) {
					if (!lastLineEmpty) {
						append("\n</p>\n<p>");
					}
					lastLineEmpty = true;
				} else {
					appendEscaped(line, searchTerms);
					lastLineEmpty = false;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("should not get IOException on a StringReader", e);
		}
		append("</p>\n");
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
	
	public Appendable append(char c) {
		preAppend();
		builder.append(c);
		
		return this;
	}
	
	public Appendable append(CharSequence cs) {
		preAppend();
		builder.append(cs);
		
		return this;
	}
	
	public Appendable append(CharSequence cs, int start, int end) {
		preAppend();
		builder.append(cs, start, end);
		
		return this;
	}
	
	@Override
	public String toString() {
		for (int i = 0; i < elementStack.size(); i++) {
			closeElement();
		}
		return builder.toString();
	}
	
	public byte[] getBytes() {
		return StringUtils.getBytes(toString());
	}
	
	public static String escape(String text) {
		XmlBuilder builder = new XmlBuilder();
		builder.appendEscaped(text);
		return builder.toString();
	}
}
