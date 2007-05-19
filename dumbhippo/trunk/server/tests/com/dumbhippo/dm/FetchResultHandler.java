package com.dumbhippo.dm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class FetchResultHandler extends DefaultHandler {
	private static final String MUGSHOT_SYSTEM_NS = "http://mugshot.org/p/system";
	
	private Locator locator;
	private State state = State.OUTSIDE;
	private FetchResult currentResult;
	private FetchResultResource currentResource;
	private FetchResultProperty currentProperty;
	private StringBuilder currentChars;
	private List<FetchResult> results = new ArrayList<FetchResult>(); 
	
	private static enum State {
		OUTSIDE,
		FETCH_RESULTS,
		FETCH_RESULT,
		RESOURCE,
		PROPERTY
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXParseException {
		switch (state) {
		case OUTSIDE:
			if (!("".equals(uri) && "fetchResults".equals(localName)))
				throw new SAXParseException("Outer element must be <fetchResults/>", locator);
			state = State.FETCH_RESULTS;
			break;
		case FETCH_RESULTS:
			startFetchResult(uri, localName, attributes);
			state = State.FETCH_RESULT;
			break;
		case FETCH_RESULT:
			startResourceElement(uri, localName, attributes);
			state = State.RESOURCE;
			break;
		case RESOURCE:
			startPropertyElement(uri, localName, attributes);
			state = State.PROPERTY;
			break;
		case PROPERTY:
			throw new SAXParseException("Elements are not allowed as children of property elements", locator);
		}
	}

	private void startFetchResult(String uri, String localName, Attributes attributes) throws SAXParseException {
		if (!("".equals(uri) && "fetchResult".equals(localName)))
			throw new SAXParseException("Only <fetchResult/> allowed as a child of <fetchResults/>", locator);

		String id = null;
		
		for (int i = 0; i < attributes.getLength(); i++) {
			if ("".equals(attributes.getURI(i)) && "id".equals(attributes.getLocalName(i)))
				id = attributes.getValue(i);
		}
		
		if (id == null)
			throw new SAXParseException("id attribute required for <fetchResult/> element", locator);
		
		currentResult = new FetchResult(id);
	}

	private void startResourceElement(String uri, String localName, Attributes attributes) throws SAXParseException {
		String fetch = null;
		String resourceId = null;
		
		for (int i = 0; i < attributes.getLength(); i++) {
			if (MUGSHOT_SYSTEM_NS.equals(attributes.getURI(i))) {
				String name = attributes.getLocalName(i);
				if ("fetch".equals(name))
					fetch = attributes.getValue(i);
				else if ("resourceId".equals(name))
					resourceId = attributes.getValue(i);
			} 
		}
		
		if (resourceId == null)
			throw new SAXParseException("mugs:resourceId attribute required on resource element", locator);
		
		currentResource = new FetchResultResource(resourceId, uri, fetch);
	}

	private void startPropertyElement(String uri, String localName, Attributes attributes) {
		String resourceId = null;
		
		for (int i = 0; i < attributes.getLength(); i++) {
			if (MUGSHOT_SYSTEM_NS.equals(attributes.getURI(i))) {
				String name = attributes.getLocalName(i);
				if ("resourceId".equals(name))
					resourceId = attributes.getValue(i);
			} 
		}
		
		if (resourceId != null)
			currentProperty = FetchResultProperty.createResource(localName, uri, resourceId);
		else {
			currentProperty = FetchResultProperty.createSimple(localName, uri);
			currentChars = new StringBuilder();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXParseException {
		if (currentChars != null) {
			currentChars.append(ch, start, length);
		} else {
			for (int i = start; i < start + length; i++) {
				if (!Character.isWhitespace(ch[i]))
					throw new SAXParseException("Unexpected non-whitespace content", locator);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (state) {
		case OUTSIDE:
			throw new RuntimeException("Unexpected end of element when nothing open");
		case FETCH_RESULTS:
			state = State.OUTSIDE;
			break;
		case FETCH_RESULT:
			results.add(currentResult);
			state = State.FETCH_RESULTS;
			break;
		case RESOURCE:
			currentResult.addResource(currentResource);
			state = State.FETCH_RESULT;
			break;
		case PROPERTY:
			if (currentChars != null) {
				currentProperty.setValue(currentChars.toString());
				currentChars = null;
			}
			state = State.RESOURCE;
			break;
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	public static SAXParserFactory saxFactory; 
	
	private static synchronized SAXParserFactory getParserFactory() {
		if (saxFactory == null) {
			saxFactory = SAXParserFactory.newInstance();
			saxFactory.setNamespaceAware(true);
			saxFactory.setValidating(false);
		}
		
		return saxFactory;
	}
	
	private static SAXParser createParser() {
		SAXParser parser;
		try {
			parser = getParserFactory().newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		
		return parser;
	}

	public static List<FetchResult> parse(InputStream input) throws SAXException, IOException {
		SAXParser parser = createParser();
		FetchResultHandler handler = new FetchResultHandler();
		
		parser.parse(input, handler);
		
		return handler.results;
	}
}
