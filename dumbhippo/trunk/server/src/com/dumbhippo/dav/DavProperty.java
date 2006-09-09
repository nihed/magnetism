package com.dumbhippo.dav;

import com.dumbhippo.XmlBuilder;

/**
 * DAV properties we understand; DAV spec suggest supporting 
 * properties we don't understand also on a "pass through unmodified" 
 * basis, if we did that they'd use a different API than an enum.
 *  
 *  Capitalization matches the XML element names.
 *  
 * @author Havoc Pennington
 */
public enum DavProperty {
	CREATION_DATE(DavXmlElement.creationdate),
	DISPLAY_NAME(DavXmlElement.displayname),
	RESOURCE_TYPE(DavXmlElement.resourcetype) {
		@Override
		public void writeNode(XmlBuilder xml, Object value) {
			DavResourceType t = (DavResourceType) value;
			t.writeResourceTypeProperty(xml);
		}
	},
	CONTENT_LANGUAGE(DavXmlElement.getcontentlanguage),
	CONTENT_TYPE(DavXmlElement.getcontenttype),
	CONTENT_LENGTH(DavXmlElement.getcontentlength),
	LAST_MODIFIED(DavXmlElement.getlastmodified) {
		@Override
		public void writeNode(XmlBuilder xml, Object value) {
			Long timestamp = (Long) value;
			xml.appendTextNode(getElement(), FastHttpDateFormat.formatDate(timestamp, null));
		}
	};

	private DavXmlElement element;
	
	private DavProperty(DavXmlElement element) {
		this.element = element;
	}
	
	public DavXmlElement getElement() {
		return element;
	}
	
	public void writeNode(XmlBuilder xml, Object value) {
		// default implementation
		if (value == null) {
			throw new RuntimeException("don't know how to write null node for " + this.name());
		} else if (value instanceof String) {
			xml.appendTextNode(element, (String) value);
		} else if (value instanceof Number) {
			xml.appendLongNode(element, ((Number) value).longValue());
		} else if (value instanceof Boolean) {
			xml.appendBooleanNode(element, (Boolean) value);
		} else {
			throw new RuntimeException("don't know how to write node " + this.name() + " of type " + value.getClass().getName());
		}
	}
	
	static public DavProperty fromElementName(String elementName) throws IllegalArgumentException {
		for (DavProperty prop : values()) {
			if (prop.getElement().name().equals(elementName))
				return prop;
		}
		throw new IllegalArgumentException("unknown property " + elementName);
	}
}
