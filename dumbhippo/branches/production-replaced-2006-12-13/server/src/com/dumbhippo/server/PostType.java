package com.dumbhippo.server;

/**
 * This enumeration distinguishes a few different ways
 * we use posts in the system.  Currently this is not
 * stored in the database persistently, but we need
 * to do a few special things if a post is a group
 * share/invitation versus a regular link post during
 * processing.
 * 
 * @author walters
 *
 */
public enum PostType {
	NORMAL,
	GROUP,
	FEED,
	TUTORIAL
}
