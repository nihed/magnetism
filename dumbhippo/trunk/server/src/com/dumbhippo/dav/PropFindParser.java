package com.dumbhippo.dav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.StringUtils;

class PropFindParser extends EnumSaxHandler<DavXmlElement> {

	private static final Logger logger = GlobalSetup.getLogger(PropFindParser.class);
	
	private Set<String> incompletePropertiesRequested;
	private Set<String> propertiesRequested;
	private boolean allRequested;
	private boolean namesRequested;
	
	PropFindParser() {
		super(DavXmlElement.class, DavXmlElement.IGNORED);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//logger.debug("uri = {} localName = {}", uri, localName);
		if (incompletePropertiesRequested != null) {
			incompletePropertiesRequested.add(qName);
		}
		super.startElement(uri, localName, qName, attributes);
	}
	
	@Override
	protected void openElement(DavXmlElement element) throws SAXException {
		boolean parentIsPropfind = false;
		switch (element) {
		// root element
		case propfind:
			break;
		// get specific props, to be listed as children of this
		case prop:
			parentIsPropfind = true;
			if (incompletePropertiesRequested != null)
				throw new SAXException("nested <prop> elements?");
			if (propertiesRequested != null)
				throw new SAXException("multiple <prop> elements?");
			incompletePropertiesRequested = new HashSet<String>();
			break;
		// get all props
		case allprop:	
			parentIsPropfind = true;
			allRequested = true;
			break;
		// list property names
		case propname:
			parentIsPropfind = true;
			namesRequested = true;
			break;
		default:
			break;
		}
		
		if (parentIsPropfind && parent() != DavXmlElement.propfind)
			throw new SAXException("<" + element.name() + "> not inside <propfind>");
	}
	
	@Override
	protected void closeElement(DavXmlElement element) throws SAXException {
		if (element == DavXmlElement.prop) {
			propertiesRequested = incompletePropertiesRequested;
			incompletePropertiesRequested = null;
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		int requests = 0;
		if (propertiesRequested != null)
			requests += 1;
		if (allRequested)
			requests += 1;
		if (namesRequested)
			requests += 1;
		if (requests == 0)
			throw new SAXException("PROPFIND didn't ask to do anything");
		else if (requests > 1)
			throw new SAXException("Multiple requests in the same PROPFIND");
	}

	public boolean isAllRequested() {
		return allRequested;
	}

	public boolean isNamesRequested() {
		return namesRequested;
	}

	public Set<String> getPropertiesRequested() {
		return propertiesRequested;
	}
	
	@Override
	public void parse(InputStream in) throws IOException, SAXException {
		if (logger.isDebugEnabled()) {
			byte[] bytes = StreamUtils.readStreamBytes(in, Long.MAX_VALUE);
			logger.debug("got propfind xml '{}'", new String(bytes, "UTF-8"));
			super.parse(new ByteArrayInputStream(bytes));
		} else {
			super.parse(in);
		}
	}
}
