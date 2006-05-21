package com.dumbhippo.persistence;

/**
 * The database uses the ordinals and the xmpp message format uses the 
 * names, so just don't change any existing values in this enum.
 * We need an UNKNOWN value instead of using null since it's mapped to
 * an int in the db and thus can't be nullable with EJB3 persistence.
 * 
 * The client also has these strings, and they have to be in sync.
 * 
 * @author hp
 */
public enum MediaFileFormat {
	UNKNOWN,
	MP3,
	WMA,
	AAC,
	VORBIS;
}
