package com.dumbhippo.dav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.dumbhippo.StringUtils;
import com.dumbhippo.server.NotFoundException;

public class DavTestNode implements DavNode {

	final static long TIMESTAMP = System.currentTimeMillis();
	
	private String name; // null for root
	private Map<String,DavNode> children;
	private Map<DavProperty,Object> properties;
	private DavNode parent;
	private byte[] fileContents;
	
	private void setParent(DavNode parent) {
		this.parent = parent;
	}
	
	public DavTestNode(String name) {
		this.name = name;
		this.children = null;
		this.fileContents = StringUtils.getBytes("These are the contents of the file named '" + name + "'");
	}
	
	public DavTestNode(String name, Collection<DavTestNode> children) {
		this.name = name;
		this.children = new HashMap<String,DavNode>();
		for (DavTestNode child : children) {
			this.children.put(child.getName(), child);
			child.setParent(this);
		}
		this.fileContents = null;
	}
	
	public String getName() {
		return name;
	}

	public Map<DavProperty, Object> getProperties() {
		if (properties == null) {
			properties = new EnumMap<DavProperty,Object>(DavProperty.class);
			if (getResourceType() != DavResourceType.COLLECTION) {
				properties.put(DavProperty.CONTENT_LENGTH, getContentLength());
				properties.put(DavProperty.CONTENT_TYPE, getContentType());
			}
			properties.put(DavProperty.DISPLAY_NAME, getDisplayName());
			properties.put(DavProperty.RESOURCE_TYPE, getResourceType());
			properties.put(DavProperty.LAST_MODIFIED, getLastModified());
		}
		return properties;
	}

	public DavNode getParent() {
		return parent;
	}

	public Collection<DavNode> getChildren() {
		if (children != null)
			return children.values();
		else
			return Collections.emptySet();
	} 
	
	public DavNode getChild(String name) throws NotFoundException {
		if (children == null)
			throw new NotFoundException("not a collection");
		
		DavNode child = children.get(name);
		if (child == null)
			throw new NotFoundException("no child '" + name + "' underneath '" + (parent != null ? parent.getName() : "root") + "'");
		else
			return child;
	}
	
	public void write(OutputStream out) throws IOException {
		switch (getResourceType()) {
		case COLLECTION:
			throw new RuntimeException("should not be trying to write the content of a collection");
		case FILE:
			out.write(fileContents);
			out.flush();
			break;
		}
	}

	public int getContentLength() {
		switch (getResourceType()) {
		case COLLECTION:
			throw new RuntimeException("should not be trying to get content length of a collection");
		case FILE:
			return fileContents.length;
		}
		throw new RuntimeException("bad resource type?");
	}

	public String getContentType() {
		switch (getResourceType()) {
		case COLLECTION:
			throw new RuntimeException("should not be trying to get content type of a collection");
		case FILE:
			return "text/plain";
		}
		throw new RuntimeException("bad resource type?");
	}
	
	public DavResourceType getResourceType() {
		if (children != null)
			return DavResourceType.COLLECTION;
		else
			return DavResourceType.FILE;
	}
	
	public String getDisplayName() {
		return name != null ? getName() : "Server Root";		
	}
	
	public long getLastModified() {
		return TIMESTAMP;
	}

	public void replaceContent(String mimeType, InputStream contents) throws IOException {
		throw new IOException("Not a writable file");
	}
	
	public void delete() throws IOException {
		throw new IOException("Not a writable folder");
	}
	
	public void createChild(String name, String mimeType, InputStream contents) throws IOException {
		throw new IOException("Not a writable folder");
	}
}
