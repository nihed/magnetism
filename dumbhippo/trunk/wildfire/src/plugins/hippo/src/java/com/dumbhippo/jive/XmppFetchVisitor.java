package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.PlainPropertyHolder;
import com.dumbhippo.dm.schema.ResourcePropertyHolder;

public class XmppFetchVisitor implements FetchVisitor {
	private Element rootElement;
	private Element currentResourceElement;
	private Map<String, Namespace> seenNamespaces = new HashMap<String, Namespace>();
	private Set<DMPropertyHolder> seenProperties = new HashSet<DMPropertyHolder>();
	
	private static final Namespace SYSTEM_NAMESPACE = Namespace.get("m", "http://mugshot.org/p/system");
	
	private static final QName RESOURCE_ID_QNAME = QName.get("resourceId", SYSTEM_NAMESPACE);
	private static final QName FETCH_QNAME = QName.get("fetch", SYSTEM_NAMESPACE);
	private static final QName INDIRECT_QNAME = QName.get("indirect", SYSTEM_NAMESPACE);
	private static final QName CARDINALITY_QNAME = QName.get("cardinality", SYSTEM_NAMESPACE);
	private static final QName UPDATE_QNAME = QName.get("update", SYSTEM_NAMESPACE);
	
	public XmppFetchVisitor(Element rootElement, DataModel model) {
		this.rootElement = rootElement;
		
		DocumentFactory factory = DocumentFactory.getInstance();
		
		rootElement.add(SYSTEM_NAMESPACE);
		
		QName resourceBaseAttr = factory.createQName("resourceBase", SYSTEM_NAMESPACE);
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
			seenNamespaces.put(namespace, ns);
		}
		
		return DocumentFactory.getInstance().createQName(localname, ns);
	}
	
	public <K, T extends DMObject<K>> void beginResource(DMClassHolder<K, T> classHolder, K key, String fetchString, boolean indirect) {
		currentResourceElement = rootElement.addElement(createQName("resource", classHolder.getClassId()));
		currentResourceElement.addAttribute(RESOURCE_ID_QNAME, classHolder.makeRelativeId(key));
		currentResourceElement.addAttribute(FETCH_QNAME, fetchString);
		if (indirect)
			currentResourceElement.addAttribute(INDIRECT_QNAME, "true");
	}

	public void endResource() {
		currentResourceElement = null;
		seenProperties.clear();
	}
	
	private Element addPropertyElement(DMPropertyHolder propertyHolder) {
		Element element = currentResourceElement.addElement(createQName(propertyHolder.getName(), propertyHolder.getNameSpace()));
		
		if (seenProperties.contains(propertyHolder))
			element.addAttribute(UPDATE_QNAME, "add");
		else
			seenProperties.add(propertyHolder);
		
		element.addAttribute(CARDINALITY_QNAME, propertyHolder.getCardinality().getValue());
		
		return element;
	}

	public void plainProperty(PlainPropertyHolder propertyHolder, Object value) {
		Element element = addPropertyElement(propertyHolder);
			
		element.addAttribute(CARDINALITY_QNAME, propertyHolder.getCardinality().getValue());
		element.addText(value.toString());
	}

	public <KP, TP extends DMObject<KP>> void resourceProperty(ResourcePropertyHolder<?, ?, KP, TP> propertyHolder, KP key) {
		Element element = addPropertyElement(propertyHolder);

		DMClassHolder<KP,TP> classHolder = propertyHolder.getResourceClassHolder();
		element.addAttribute(RESOURCE_ID_QNAME, classHolder.makeRelativeId(key));
	}

	public void emptyProperty(DMPropertyHolder propertyHolder) {
		Element element = currentResourceElement.addElement(createQName(propertyHolder.getName(), propertyHolder.getNameSpace()));
		
		element.addAttribute(UPDATE_QNAME, "clear");
		element.addAttribute(CARDINALITY_QNAME, propertyHolder.getCardinality().getValue());
	}
}
