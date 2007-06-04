package com.dumbhippo.dm;

/**
 * The DMSessionMap interface represents operations that the environment provides to {@link DataModel}
 * for manipulating transactions. In particular, it handles keeping a mapping between the current
 * transaction and the session for the transaction.
 * 
 * @author otaylor
 */
public interface DMSessionMap {
	public void initCurrent(DMSession session);
	public DMSession getCurrent();
	public void runInTransaction(Runnable runnable);
}
