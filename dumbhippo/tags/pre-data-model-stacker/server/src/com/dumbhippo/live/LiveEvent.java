package com.dumbhippo.live;

import java.io.Serializable;

/**
 * Base type for events that are used to notify the live 
 * state cache of changes to the underlying data.
 * 
 * @author otaylor
 */
public abstract interface LiveEvent extends Serializable {
	static public final String TOPIC_NAME = "topic/LiveUpdateTopic";
	
	/**
	 * Get the type of the Stateless session bean that will be
	 * used to process incoming events of this event type and
	 * update the state cache.
	 * 
	 * @return the class object. The session bean will be looked
	 *   up in JNDI under the fully qualified name of this class.
	 *   (A @LocalBinding annotation on the session bean is needed
	 *   to make that work.)
	 */
	public Class<? extends LiveEventProcessor> getProcessorClass();
}
