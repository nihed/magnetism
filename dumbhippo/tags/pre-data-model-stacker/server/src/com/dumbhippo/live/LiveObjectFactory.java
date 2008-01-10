package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * This interface represents an object that creates a particular type
 * of LiveObject based on the guid of the object.
 *
 * @author otaylor
 * @param <T>
 */
interface LiveObjectFactory<T extends LiveObject> {
	T create(Guid guid);
}
