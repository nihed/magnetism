package com.dumbhippo.dav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import com.dumbhippo.server.NotFoundException;

public interface DavNode {

	/**
	 * Get the name of the node, NOT url-encoded or in any way url-aware
	 * @return
	 */
	String getName();
	
	Map<DavProperty,Object> getProperties();

	DavNode getParent();
	
	Collection<DavNode> getChildren();
	
	DavNode getChild(String name) throws NotFoundException;
	
	void write(OutputStream out) throws IOException;
	
	int getContentLength();
	
	DavResourceType getResourceType();
	
	String getContentType();

	String getDisplayName();
	
	long getLastModified();
}
