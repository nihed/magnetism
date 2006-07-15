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
	public void appendMember(String name, String value) {
		appendTextNode(name, value);
	}
	
	public void appendMember(String name, boolean value) {
		appendMember(name, value ? "true" : "false");
	}
	
	public void appendAttribute(String name, long value) {
		appendMember(name, new Long(value).toString());
	}
}
