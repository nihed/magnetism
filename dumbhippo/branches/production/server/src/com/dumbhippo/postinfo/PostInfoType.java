package com.dumbhippo.postinfo;

/**
 * We have one enum value for each kind of post info. 
 * These values should never be removed; if you change
 * a format in a non-backward-compatible way,
 * you would add a value like "AMAZON2" including
 * a version number to indicate the new format.
 * 
 * This is a type tag instead of just using subclasses because
 * we may have multiple types mapping to one subclass in theory.
 * But mostly the static
 * information is a pain in the ass with subclasses since you 
 * can't have "static interfaces" (you would need a subclass instance
 * or reflection of static fields to get at the NodeName for the type)
 * 
 * @author hp
 */
public enum PostInfoType {
	GENERIC(NodeName.generic, GenericPostInfo.class),
	SHARE_GROUP(NodeName.shareGroup, ShareGroupPostInfo.class),
	EBAY(NodeName.eBay, EbayPostInfo.class),
	AMAZON(NodeName.amazon, AmazonPostInfo.class),
	FLICKR(NodeName.flickr, FlickrPostInfo.class);	
	
	private NodeName nodeName;
	private Class<? extends PostInfo> subClass;
	
	private <T extends PostInfo> PostInfoType(NodeName nodeName, Class<T> subClass) {
		this.nodeName = nodeName;
		this.subClass = subClass;
	}
	
	public NodeName getNodeName() {
		return nodeName;
	}
	
	public Class<? extends PostInfo> getSubClass() {
		return subClass;
	}
	
	static public PostInfoType fromNodeName(NodeName nodeName) {
		for (PostInfoType t : values()) {
			if (t.getNodeName() == nodeName)
				return t;
		}
		return null;
	}
}
