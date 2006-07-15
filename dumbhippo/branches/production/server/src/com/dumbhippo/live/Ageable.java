package com.dumbhippo.live;

/**
 * Common interface by the objects we keep with an age in the 
 * LiveState cache. We could keep the age in a separate cache-node
 * helper object rather than in the object itself, but this is 
 * a little more efficient. 
 * 
 * @author otaylor
 */
public interface Ageable {
	public int getCacheAge();
	public void setCacheAge(int age);

	/**
	 * Discard any resources associated with this object, when
	 * it is aged out of the cache. This is most sensible in
	 * the case (LiveXmppServer) where we don't keep a 
	 * weak-reference map to resurrect the object.
	 */
	public void discard();
}
