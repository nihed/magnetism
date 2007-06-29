package com.dumbhippo.server;

/**
 * This is to be sure getFooEnabled() calls aren't misused; there are 
 * two contexts, to display the current preference we want to just look
 * at a single database field, to decide whether to really enable something
 * we also need to look at whether the account is live.
 * 
 * Kind of gross, but otherwise it's too easy to mess this up.
 * 
 * @author Havoc Pennington
 */
public enum Enabled {
	RAW_PREFERENCE_ONLY,
	AND_ACCOUNT_IS_ACTIVE
}
