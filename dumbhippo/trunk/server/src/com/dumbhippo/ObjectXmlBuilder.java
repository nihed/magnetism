/**
 * 
 */
package com.dumbhippo;


/**
 * 
 * Basic braindead XML object-attribute serializer.
 * 
 * @author walters
 *
 */
public class ObjectXmlBuilder extends XmlBuilder {
	public void appendAttribute(String name, String value) {
		appendTextNode(name, value);
	}
	
	public void appendAttribute(String name, boolean value) {
		appendAttribute(name, value ? "true" : "false");
	}
	
	public void appendAttribute(String name, long value) {
		appendAttribute(name, new Long(value).toString());
	}
}
