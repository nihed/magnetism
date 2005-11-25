package com.dumbhippo.server.rewriters;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

abstract class EnumSaxHandler<E extends Enum<E>> extends DefaultHandler {
	private Class<E> enumClass;
	private E ignoredValue;
	
	private List<E> stack;
	private StringBuilder content;

	protected EnumSaxHandler(Class<E> enumClass, E ignoredValue) {
		this.enumClass = enumClass;
		this.ignoredValue = ignoredValue;
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
	
	private void push(String name) {
		 E value = parseElementName(name);
		 stack.add(value);			
	}
	
	private void pop(String name) throws SAXException {
		 E value = parseElementName(name);
		 E c = current();
		 if (c == null)
			 throw new SAXException("popped " + name + " when not in an E");
		 if (c != value)
			 throw new SAXException("unmatched close to E " + name + " we werE valuexpecting " + c.name());
		 stack.remove(stack.size() - 1);
		 content.setLength(0);
	}
	
	@Override
	public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		 //logger.debug("start E " + qName);
		 push(qName);
	}
	
	@Override
	public final void endElement(String uri, String localName, String qName) throws SAXException {
		E c = current();

		handleElement(c);
		
		pop(qName);
	}
	
	@Override
	public final void characters(char[] ch, int start, int length) throws SAXException {
		E c = current();
		if (c != ignoredValue)
			content.append(ch, start, length);
	}
	
	protected final E current() {
		if (stack.size() > 0)
			return stack.get(stack.size() - 1);
		else
			return null;
	}

	protected final E parent() {
		if (stack.size() > 1)
			return stack.get(stack.size() - 2);
		else
			return null;
	}
	
	protected String getCurrentContent() {
		return content.toString();
	}
	
	protected abstract void handleElement(E element) throws SAXException;
}
