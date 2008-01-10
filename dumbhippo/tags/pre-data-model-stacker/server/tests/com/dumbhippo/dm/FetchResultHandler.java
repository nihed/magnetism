package com.dumbhippo.dm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.dumbhippo.GlobalSetup;

public class FetchResultHandler extends DefaultHandler {
	public static final Logger logger = GlobalSetup.getLogger(FetchResultHandler.class);
	
	public static final String MUGSHOT_SYSTEM_NS = "http://mugshot.org/p/system";
	
	private Locator locator;
	private State state = State.OUTSIDE;
	private FetchResult currentResult;
	private FetchResultResource currentResource;
	private FetchResultProperty currentProperty;
	private StringBuilder currentChars;
	private List<FetchResult> results = new ArrayList<FetchResult>();
	private List<URI> resourceBaseStack = new ArrayList<URI>();
	private URI resourceBase;
	
	private static enum State {
		OUTSIDE,
		FETCH_RESULTS,
		FETCH_RESULT,
		RESOURCE,
		PROPERTY
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXParseException {
		resourceBaseStack.add(resourceBase);
		for (int i = 0; i < attributes.getLength(); i++) {
			if (MUGSHOT_SYSTEM_NS.equals(attributes.getURI(i))) {
				String name = attributes.getLocalName(i);
				if ("resourceBase".equals(name)) {
					try {
						resourceBase = new URI(attributes.getValue(i));
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						throw new SAXParseException("resourceBase element must be a valid URI", locator);
					}
				}
			} 
		}
		
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
	
	private String resolveResourceId(String resourceId) throws SAXParseException {
		if (resourceBase == null)
			return resourceId;
		else {
			try {
				return resourceBase.resolve(resourceId).toString();
			} catch (IllegalArgumentException e) {
				logger.debug("Cannot resolve resource {}: {}", resourceId, e);
				throw new SAXParseException("Cannot resolve resourceId against current resourceBase", locator);
			}
		}
	}
	
	private void startResourceElement(String uri, String localName, Attributes attributes) throws SAXParseException {
		String fetch = null;
		String resourceId = null;
		String classId = null;
		boolean indirect = false;
		
		if (!"resource".equals(localName))
			throw new SAXParseException("Local name of resource element must be 'resource'", locator);
		
		for (int i = 0; i < attributes.getLength(); i++) {
			if (MUGSHOT_SYSTEM_NS.equals(attributes.getURI(i))) {
				String name = attributes.getLocalName(i);
				String value = attributes.getValue(i);
				
				if ("fetch".equals(name))
					fetch = value;
				else if ("classId".equals(name))
					classId = value;
				else if ("resourceId".equals(name))
					resourceId = resolveResourceId(value);
				else if ("indirect".equals(name)) {
					indirect = Boolean.valueOf(value);
				}
			} 
		}
		
		if (MUGSHOT_SYSTEM_NS.equals(uri)) {
			if (classId == null)
				throw new SAXParseException("classId argument mandatory for <mugs:resource/> element", locator);
		} else if (uri != null) {
			if (classId != null)
				throw new SAXParseException("classId argument present on namespaced resource <resource/> element", locator);
			classId = uri;
		} else {
			throw new SAXParseException("no namespace for <resource/> element", locator);
		}
		
		if (classId.contains("#"))
			throw new SAXParseException("property class ID can't have a fragment identifier", locator);
		
		if (resourceId == null)
			throw new SAXParseException("mugs:resourceId attribute required on resource element", locator);
		
		currentResource = new FetchResultResource(classId, resourceId, fetch, indirect);
	}

	private void startPropertyElement(String uri, String localName, Attributes attributes) throws SAXParseException {
		String resourceId = null;
		long timestamp = -1;
		
		for (int i = 0; i < attributes.getLength(); i++) {
			if (MUGSHOT_SYSTEM_NS.equals(attributes.getURI(i))) {
				String name = attributes.getLocalName(i);
				if ("resourceId".equals(name))
					resourceId = resolveResourceId(attributes.getValue(i));
				else if ("ts".equals(name))
					timestamp = Long.parseLong(attributes.getValue(i));
				
			} 
		}
		
		if (resourceId != null) {
			if (timestamp >= 0)
				currentProperty = FetchResultProperty.createFeed(localName, uri, resourceId, timestamp);
			else
				currentProperty = FetchResultProperty.createResource(localName, uri, resourceId);
		} else {
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
			currentResult = null;
			state = State.FETCH_RESULTS;
			break;
		case RESOURCE:
			currentResult.addResource(currentResource);
			currentResource = null;
			state = State.FETCH_RESULT;
			break;
		case PROPERTY:
			if (currentChars != null) {
				currentProperty.setValue(currentChars.toString());
				currentChars = null;
			}
			currentResource.addProperty(currentProperty);
			currentProperty = null;
			state = State.RESOURCE;
			break;
		}
		
		resourceBase = resourceBaseStack.remove(resourceBaseStack.size() - 1);
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
