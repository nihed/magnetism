package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;

public class XmppFetchVisitor implements FetchVisitor {
	private Element rootElement;
	private Element currentResourceElement;
	private Map<String, Namespace> seenNamespaces = new HashMap<String, Namespace>();
	private Namespace systemNamespace;
	private QName resourceIdAttr;
	private QName fetchAttr;
	private QName indirectAttr;
	
	public XmppFetchVisitor(Element rootElement, DataModel model) {
		this.rootElement = rootElement;
		
		DocumentFactory factory = DocumentFactory.getInstance();
		
		systemNamespace = factory.createNamespace("m", "http://mugshot.org/p/system");
		rootElement.add(systemNamespace);
		
		resourceIdAttr = factory.createQName("resourceId", systemNamespace);
		fetchAttr = factory.createQName("fetch", systemNamespace);
		indirectAttr = factory.createQName("indirect", systemNamespace);
		
		QName resourceBaseAttr = factory.createQName("resourceBase", systemNamespace);
		rootElement.addAttribute(resourceBaseAttr, model.getBaseUrl());
	}
	
	public Element getRootElement() {
		return rootElement;
	}

	public boolean getNeedFetch() {
		return true;
	}

	private QName createQName(String localname, String namespace) {
		Namespace ns = seenNamespaces.get(namespace);
		
		if (ns == null) {
			String prefix = "r" + (seenNamespaces.size() + 1);
			ns = DocumentFactory.getInstance().createNamespace(prefix, namespace);
			rootElement.add(ns);
		}
		
		return DocumentFactory.getInstance().createQName(localname, ns);
	}
	
	public <K, T extends DMObject<K>> void beginResource(DMClassHolder<K, T> classHolder, K key, String fetchString, boolean indirect) {
		currentResourceElement = rootElement.addElement(createQName("resource", classHolder.getClassId()));
		currentResourceElement.addAttribute(resourceIdAttr, classHolder.makeRelativeId(key));
		currentResourceElement.addAttribute(fetchAttr, fetchString);
		if (indirect)
			currentResourceElement.addAttribute(indirectAttr, "true");
	}

	public void endResource() {
		currentResourceElement = null;
	}

	public void plainProperty(PlainPropertyHolder propertyHolder, Object value) {
		Element element = rootElement.addElement(createQName(propertyHolder.getName(), propertyHolder.getNameSpace()));
		element.addText(value.toString());
	}

	public <KP, TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<?, ?, KP, TP> propertyHolder, KP key) {
		DMClassHolder<KP,TP> classHolder = propertyHolder.getResourceClassHolder();
		Element element = rootElement.addElement(createQName(propertyHolder.getName(), propertyHolder.getNameSpace()));
		element.addAttribute(resourceIdAttr, classHolder.makeRelativeId(key));
	}
}
