package com.dumbhippo.dav;

/**
 * EnumSaxHandler-compatible enum of DAV XML elements.
 * 
 * @author Havoc Pennington
 */
public enum DavXmlElement {
	activelock,
	depth,
	locktoken,
	timeout,
	collection,
	href,
	link,
	dst,
	src,
	lockentry,
	lockinfo,
	lockscope,
	exclusive,
	shared,
	locktype,
	write,
	multistatus,
	response,
	propstat,
	status,
	responsedescription,
	owner,
	prop,
	propertybehavior,
	keepalive,
	omit,
	propertyupdate,
	remove,
	set,
	propfind,
	allprop,
	propname,
	creationdate,
	displayname,
	getcontentlanguage,
	getcontentlength,
	getcontenttype,
	getetag,
	getlastmodified,
	lockdiscovery,
	resourcetype,
	source,
	supportedlock,
	IGNORED;
}
