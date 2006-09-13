package com.dumbhippo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public abstract class EnumSaxHandler<E extends Enum<E>> extends DefaultHandler {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(EnumSaxHandler.class);

	private static SAXParserFactory saxFactory;	
	
	private Class<E> enumClass;
	private E ignoredValue;
	
	private List<Attributes> attributesStack;
	private List<E> stack;
	private StringBuilder content;

	// putting these general SAX convenience methods in this class is a little bogus, but 
	// oh well
	
	public static SAXParser newSAXParser() {
		SAXParser parser;
		try {
			synchronized (EnumSaxHandler.class) {
				if (saxFactory == null) {
					saxFactory = SAXParserFactory.newInstance();
					saxFactory.setNamespaceAware(true);
					saxFactory.setValidating(false);
					logger.debug("New SAX parser factory validating {} namespaces {}",
							saxFactory.isValidating(), saxFactory.isNamespaceAware());				}
				
				parser = saxFactory.newSAXParser();
			}
		} catch (ParserConfigurationException e) {
			logger.error("failed to create sax parser: {}", e.getMessage());
			throw new RuntimeException(e);
		} catch (SAXException e) {
			logger.error("failed to create sax parser: {}", e.getMessage());
			throw new RuntimeException(e);
		}
		return parser;
	}
	
	public static void parse(InputStream input, DefaultHandler handler)
			throws SAXException, IOException {
		newSAXParser().parse(input, handler);
	}
	
	public static void parse(String input, DefaultHandler handler)
		throws SAXException, IOException {
		newSAXParser().parse(new InputSource(new StringReader(input)), handler);
	}
	
	public void parse(InputStream input) throws SAXException, IOException {
		parse(input, this);
	}
	
	public void parse(String input) throws SAXException, IOException {
		parse(input, this);
	}
	
	protected EnumSaxHandler(Class<E> enumClass, E ignoredValue) {
		this.enumClass = enumClass;
		this.ignoredValue = ignoredValue;
		attributesStack = new ArrayList<Attributes>();
		stack = new ArrayList<E>();
		content = new StringBuilder();
	}
	
	private E parseElementName(String name) {
		 E value;
		 try {
			 value = Enum.valueOf(enumClass, name);
		 } catch (IllegalArgumentException e) {
			 value = ignoredValue;
		 }
		 assert value != null;
		 return value;
	}
	
	private void push(String name, Attributes attributes) {
		 E value = parseElementName(name);

		 // The SAX parser seems to reuse or modify Attributes
		 // once startElement returns; I can't find anything about this
		 // in the docs and I think it's busted, but it apparently does 
		 // it. So make a copy. But, for the usual case where there are 
		 // no attributes, don't bother; this could leave us with 
		 // wrong attributes we didn't expect, but we usually ignore
		 // unexpected attributes so for now take the risk.
		 if (attributes.getLength() > 0)
			 attributes = new  AttributesImpl(attributes);

		 attributesStack.add(attributes);
		 stack.add(value);
	}
	
	private void pop(String name) throws SAXException {
		 E value = parseElementName(name);
		 E c = current();
		 if (c == null)
			 throw new SAXException("popped " + name + " when not in an element");
		 if (c != value)
			 throw new SAXException("unmatched close to element " + name + " we were expecting " + c.name());
		 stack.remove(stack.size() - 1);
		 attributesStack.remove(attributesStack.size() - 1);
		 content.setLength(0);
	}
	
	static private <T> T stackTop(List<T> stack) {
		if (stack.size() > 0)
			return stack.get(stack.size() - 1);
		else
			return null;		
	}
	
	// FIXME check that the uri is the right namespace if using localName
	protected String getUnqualifiedName(String uri, String localName, String qName) throws SAXException {
		if (localName != null && localName.length() > 0)
			return localName;
		else if (qName != null && qName.length() > 0) {
			int colon = qName.indexOf(':');
			if (colon >= 0)
				return qName.substring(colon);
			else
				return qName;
		} else {
			throw new SAXException("element has no name, uri=" + uri + " localName=" + localName + " qName=" + qName);
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		 //logger.debug("start element uri '{}' localName '{}' qName '" + qName + "'", uri, localName);
		 // debugLogAttributes(attributes);
		 
		 push(getUnqualifiedName(uri, localName, qName), attributes);
		 
		 openElement(current());
	}
	
	@Override
	public final void endElement(String uri, String localName, String qName) throws SAXException {
		
		E c = current();

		// logger.debug("end element " + qName + " content = '" + getCurrentContent() + "'");
		
		closeElement(c);
		
		pop(getUnqualifiedName(uri, localName, qName));
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		E c = current();
		if (c != ignoredValue)
			content.append(ch, start, length);
	}
	
	protected final E current() {
		return stackTop(stack);
	}

	protected final Attributes currentAttributes() {
		return stackTop(attributesStack);
	}
	
	protected final E parent() {
		if (stack.size() > 1)
			return stack.get(stack.size() - 2);
		else
			return null;
	}
	
	protected final boolean currentlyInside(E element) {
		return stack.contains(element);
	}
	
	protected String getCurrentContent() {
		return content.toString();
	}

	protected void clearContent() {
		content.setLength(0);
	}
	
	protected void openElement(E element) throws SAXException {
		
	}
	
	protected abstract void closeElement(E element) throws SAXException;
	
	protected void debugLogAttributes(Attributes attributes) {
		if (attributes.getLength() == 0)
			logger.debug(" (no attributes)");
		for (int i = 0; i < attributes.getLength(); ++i) {
			logger.debug(" " + i + " {} = {}", attributes.getQName(i), attributes.getValue(i));
		}
	}
}
