package com.dumbhippo;

/**
 * Allows keeping track of which info source contains information that we can display 
 * to the user when they are reviewing their info for different external accounts.
 */
public enum ExternalAccountInfoSource {
	HANDLE,
	EXTRA,
	LINK,
	FEED,
	CUSTOM;
}
