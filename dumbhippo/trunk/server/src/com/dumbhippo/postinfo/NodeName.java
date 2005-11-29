package com.dumbhippo.postinfo;

/**
 * All possible kinds of node in a PostInfo document are in this 
 * enumeration. This centrally coordinates the "namespace" and 
 * makes it easy to parse. Conceptually it's as if we had one 
 * big XML schema for PostInfo documents, vs. a separate schema
 * for each possible kind of PostInfo. The enum elements match 
 * the actual names in the XML format, case sensitive.
 * 
 * The XML format we parse this from looks something like:
 * &lt;PostInfo&gt;
 *   &lt;Type&gt;AMAZON&lt;/Type&gt;
 *   &lt;Generic&gt;
 *   	&lt;Favicon&gt;http://blahblah/favicon.ico&lt;/Favicon&gt;
 *   &lt;/Generic&gt;
 *   &lt;Amazon&gt;
 *   	&lt;SmallPhoto&gt;
 *   		&lt;Url&gt;http://blah/blah.png&lt;/Url&gt;
 *          &lt;Width&gt;48&lt;/Width&gt;
 *          &lt;Height&gt;48&lt;/Height&gt;
 *   	&lt;/SmallPhoto&gt;
 *   &lt;/Amazon&gt;
 * &lt;/PostInfo&gt;
 * 
 * The basic format of the entire document in other words is:
 *   1 type tag
 *   1 generic section
 *   1 type-specific section
 * 
 * There's no arbitrarily-deep inheritance or anything like that.
 * There are no attributes on elements either, only nodes.
 * 
 * @author hp
 */
public enum NodeName {
	// nodes in all post info documents
	PostInfo,
	Type,
	Generic,
	// type-specific sections
	Error,
	Amazon,
	eBay,
	// nodes underneath one of the above sections
	Favicon,
	SmallPhoto,
	Url,
	Width,
	Height,
	//	a node we don't know about, used in parsing only
	IGNORED 
}
