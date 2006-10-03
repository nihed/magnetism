package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;

/**
 * This class represents a list of object views ready to be serialized into XML.
 * The included views are closed under "references" and views for objects that are 
 * referenced always precede the referenced objects. ViewStreams are created using 
 * ViewStreamBuilder.
 * 
 * @author otaylor
 */
public class ViewStream {
	List<ObjectView> views = new ArrayList<ObjectView>();
	Set<Guid> viewIds = new HashSet<Guid>(); 
	
	public boolean hasObjectView(Guid id) {
		return viewIds.contains(id);
	}
	
	public void addObjectView(ObjectView view) {
		Guid id = view.getIdentifyingGuid();
		
		if (!viewIds.contains(id)) {
			viewIds.add(id);
			views.add(view);
		}
	}
	
	public void writeToXmlBuilder(XmlBuilder builder) {
		for (ObjectView view : views)
			view.writeToXmlBuilder(builder);
	}
}
