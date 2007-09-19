package com.dumbhippo.dm;

/* Interface to allow notification of only some clients on a change. 
 */
public interface ClientMatcher {
	boolean matches(DMClient client);
}
