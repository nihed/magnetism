package com.dumbhippo;

/**
 * 
 * The server can have multiple "flavors" or "skins" with different 
 * hostnames and look&feel, the two right now are GNOME and Mugshot.
 * 
 * We treat XMPP as a third "site" right now, but it may be correct 
 * eventually to require a user connecting to XMPP to specify 
 * one of the other sites.
 *
 */
public enum Site {
	GNOME,
	MUGSHOT,
	XMPP,
	// this means the code is running from a cron job for example, so 
	// we don't have a meaningful "site" it's coming from. DO NOT just 
	// use this because you're too lazy to pass the GNOME/MUGSHOT 
	// thing down through the call stack.
	NONE 
}
