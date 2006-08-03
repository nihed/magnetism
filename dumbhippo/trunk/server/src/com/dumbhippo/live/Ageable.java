package com.dumbhippo.live;

/**
 * An object with an age. After some code rearrangement, this is only
 * used for LiveXmppServer.  
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
