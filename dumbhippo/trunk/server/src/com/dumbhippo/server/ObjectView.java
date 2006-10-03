package com.dumbhippo.server;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;

/**
 * This interface is shared among all view objects that can be written into a ViewStream.
 * 
 * @author otaylor
 */
public interface ObjectView {
	/**
	 * Get a GUID that uniquely identifies the viewed object among the set of objects
	 * that can be viewed.
	 * 
	 * @return a Guid that identifies the object that is viewed
	 */
	Guid getIdentifyingGuid();
	
	/**
	 * Write an XML representation of the object into a XML builder. Referenced objects
	 * that are returned by getReferencedObjects should be identified in the XML only
	 * by ID, and not with full contents. 
	 * 
	 * @param builder the builder to write to
	 */
	void writeToXmlBuilder(XmlBuilder builder);
	
	/**
	 * Returns a list of objects that this object references. These are the objects that
	 * must be written into a ViewStream before this object when serializing to XML.
	 * 
	 * The interface here is ugly - the returned list is heterogeneous and can contain
	 * both persistence objects (User, Resource, Post, and so forth) and "View" objects
	 * that implement ObjectView. The idea here is to reduce unnecessarily creating
	 * view objects; if this object has a view for the referenced object, it should
	 * return the view to avoid recreating a new view, but if it doesn't have a view,
	 * returning the persistence object is better, since there might already be a 
	 * view for the persistence object in the stream.
	 * 
	 * A better way of setting this up might be to make this interface something like:
	 * addReferencedObjectsToNeeded(NeededObjects needed), which would allow handling
	 * the Persistence object vs. View object duality in a type-safe manner.
	 *  
	 * @return A list of objects that the viewed objects references.
	 */
	List<Object> getReferencedObjects();
}
