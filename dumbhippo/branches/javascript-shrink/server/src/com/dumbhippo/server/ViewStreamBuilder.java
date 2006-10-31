package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.ViewStream;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface ViewStreamBuilder {
	/**
	 * Create a ViewStream that includes a specified view and views for
	 * all the objects it references.
	 * 
	 * @param viewpoint Viewpoint for which the stream is to be created
	 * @param objectView object to include in the stream
	 * @return a new ViewStream
	 */
	public ViewStream buildStream(Viewpoint viewpoint, ObjectView objectView);
	
	/**
	 * Creates a ViewStream that includes the the specified views and 
	 * views for all objects that they reference. Only one view will be
	 * included for each referenced object, even if it is referenced
	 * multiple times.
	 * 
	 * @param viewpoint Viewpoint for which the stream is to be created
	 * @param objectView object to include in the stream
	 * @return a new ViewStream
	 */
	public ViewStream buildStream(Viewpoint viewpoint, List<ObjectView> objectViews); 
}
