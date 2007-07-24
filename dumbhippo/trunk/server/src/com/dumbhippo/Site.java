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
	XMPP
}
