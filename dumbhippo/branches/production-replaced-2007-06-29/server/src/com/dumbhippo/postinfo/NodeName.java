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
 * &lt;postInfo&gt;
 *   &lt;generic&gt;
 *   	&lt;favicon&gt;http://blahblah/favicon.ico&lt;/favicon&gt;
 *   &lt;/generic&gt;
 *   &lt;amazon&gt;
 *   	&lt;smallPhoto&gt;
 *   		&lt;url&gt;http://blah/blah.png&lt;/url&gt;
 *          &lt;width&gt;48&lt;/width&gt;
 *          &lt;height&gt;48&lt;/height&gt;
 *   	&lt;/smallPhoto&gt;
 *   &lt;/amazon&gt;
 * &lt;/postInfo&gt;
 * 
 * The basic format of the entire document in other words is:
 *   1 optional generic section
 *   1 optional type-specific section
 * 
 * There's no arbitrarily-deep inheritance or anything like that.
 * There are no attributes on elements either, only nodes.
 * 
 * @author hp
 */
public enum NodeName {
	// nodes in all post info documents
	postInfo,
	generic,
	// type-specific sections
	shareGroup,
	amazon,
	eBay,
	flickr,
	// nodes underneath one of the above sections
	groupId,
	favicon,
	itemId,
	smallPhoto,
	url,
	width,
	height,
	newPrice,
	usedPrice,
	refurbishedPrice,
	collectiblePrice,
	timeLeft,
	startPrice,
	buyItNowPrice,
	photos,
	photo,
	photoPageUrl,
	photoUrl,
	photoId,
	//	a node we don't know about, used in parsing only
	IGNORED 
}
