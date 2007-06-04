package com.dumbhippo.dm;

/**
 * DMKey is a base class for all custom key types for DMOs. You can also use Guid or String
 * as a key type. A custom key type:
 * 
 * <ul>
 * <li>Must implement hashCode() and equals()</li>
 * <li>Must implement toString()</li>
 * <li>Must implement clone(). If the key is immutable and contains nothing session specific,
 *     then the clone() method can simple return 'this'. If the key has session specific
 *     data in it (such as a cached reference to an Entity Bean), the clone method should
 *     create a new object without that session specific data.</li>
 * <li>Must have a constructor that takes a single String argument that parses the Guid
 *     in the form create by toString(). In case of a bad argument the constructor
 *     should throw {@link BadIdException}</li>
 * </ul>
 * 
 * @author otaylor
 */
public abstract interface DMKey extends Cloneable {
	public DMKey clone(); 
}
