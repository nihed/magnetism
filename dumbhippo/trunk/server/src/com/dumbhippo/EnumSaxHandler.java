package com.dumbhippo;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class EnumSaxHandler<E extends Enum<E>> extends DefaultHandler {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(EnumSaxHandler.class);
	
	private Class<E> enumClass;
	private E ignoredValue;
	
	private List<Attributes> attributesStack;
	private List<E> stack;
	private StringBuilder content;

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
	
	@Override
	public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		 //logger.debug("start element " + qName);
		 push(qName, attributes);
		 
		 openElement(current());
	}
	
	@Override
	public final void endElement(String uri, String localName, String qName) throws SAXException {
		
		E c = current();

		//logger.debug("end element " + c + " content = '" + getCurrentContent() + "'");
		
		closeElement(c);
		
		pop(qName);
	}
	
	@Override
	public final void characters(char[] ch, int start, int length) throws SAXException {
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
}
