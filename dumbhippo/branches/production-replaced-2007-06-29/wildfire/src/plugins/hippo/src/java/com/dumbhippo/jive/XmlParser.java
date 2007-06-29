package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class XmlParser {
	public static Element elementFromXml(String xml) { 
		Document xmlDumpDoc;
		try {
			xmlDumpDoc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			throw new RuntimeException("failed to parse xml string: " + xml);
		}
		
		Element childElement = xmlDumpDoc.getRootElement();
		childElement.detach();
		return childElement;
	}
}
