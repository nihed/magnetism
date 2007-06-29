package com.dumbhippo.dav;

import java.io.IOException;
import java.io.InputStream;
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
	
	void replaceContent(String mimeType, InputStream contents) throws IOException;
	
	void delete() throws IOException;
	
	void createChild(String name, String mimeType, InputStream contents) throws IOException;
}
