package com.dumbhippo.live;

import javax.ejb.Local;

/**
 * Interface shared by the processor session beans for all different
 * event types.
 * 
 * @author otaylor
 */
@Local
public interface LiveEventProcessor {
	/**
	 * Update the state object for an event that notifies us of a
	 * change to the underlying data. 
	 * 
	 * @param state the state object to update 
	 * @param abstractEvent the incoming event; this should be downcast
	 *    to a particular event type.
	 */
	public void process(LiveState state, LiveEvent abstractEvent);
}
