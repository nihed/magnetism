package com.dumbhippo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/* The sane way to parse XML. */
public class XmlUtils {
	public static class XmlParseData {
		public XmlParseData(Document doc, XPath xpath) {
			this.doc = doc;
			this.xpath = xpath;
		}
		public Document doc;
		public XPath xpath;
	}
	public static XmlParseData parseXml(InputSource source, final Map<String,String> namespaces) throws SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document doc;		
		try {
			doc = factory.newDocumentBuilder().parse(source);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		XPath xpath = XPathFactory.newInstance().newXPath();
		NamespaceContext nshandler = new javax.xml.namespace.NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				return namespaces == null ? null : namespaces.get(prefix);
			}
			public Iterator<?> getPrefixes(String val) { return null; }
			public String getPrefix(String uri) { return null; }
		};
		xpath.setNamespaceContext(nshandler);
		return new XmlParseData(doc, xpath);
	}
	
	private static Map<String,String> stringsToMap(String[] array) {
		if (array.length % 2 != 0)
			throw new IllegalArgumentException("Invalid mapping arrray literal; must have an even number of items");
		Map<String,String> map = new HashMap<String, String>();
		for (int i = 0; i < array.length; i+=2) {
			map.put(array[i], array[i]+1);
		}
		return map;
	}
	
	public static XmlParseData parseXml(InputStream stream, Map<String,String> namespaces) throws SAXException, IOException {
		return parseXml(new InputSource(stream), namespaces);
	}
	
	public static XmlParseData parseXml(Reader stream, Map<String,String> namespaces) throws SAXException, IOException {
		return parseXml(new InputSource(stream), namespaces);
	}
	
	public static XmlParseData parseXml(InputStream stream, String[] namespaces) throws SAXException, IOException {
		return parseXml(new InputSource(stream), stringsToMap(namespaces));
	}
	
	public static XmlParseData parseXml(Reader stream, String[] namespaces) throws SAXException, IOException {
		return parseXml(new InputSource(stream), stringsToMap(namespaces));
	}	
	
	public static XmlParseData parseXml(InputStream stream) throws SAXException, IOException {
		return parseXml(new InputSource(stream), null);
	}
	
	public static XmlParseData parseXml(Reader stream) throws SAXException, IOException {
		return parseXml(new InputSource(stream), null);
	}
}
