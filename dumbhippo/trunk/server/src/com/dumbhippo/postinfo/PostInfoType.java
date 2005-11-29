package com.dumbhippo.postinfo;

/**
 * We have one enum value for each kind of post info. 
 * These values should never be removed; if you change
 * a format in a non-backward-compatible way,
 * you would add a value like "AMAZON2" including
 * a version number to indicate the new format.
 * 
 * @author hp
 */
public enum PostInfoType {
	GENERIC,
	ERROR,
	EBAY,
	AMAZON,
	FLICKR
}
