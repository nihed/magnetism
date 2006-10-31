package com.dumbhippo.dav;

import com.dumbhippo.XmlBuilder;

/**
 * Capitalization matches the dav XML node names.
 * 
 * @author Havoc Pennington
 */
public enum DavResourceType {
	COLLECTION(DavXmlElement.collection),
	FILE(null);
	
	private DavXmlElement element;
	
	private DavResourceType(DavXmlElement element) {
		this.element = element;
	}

	public void writeResourceTypeProperty(XmlBuilder xml) {
		xml.openElement(DavXmlElement.resourcetype);
		if (element != null) {
			xml.appendEmptyNode(element);
		}
		xml.closeElement();
	}
}
