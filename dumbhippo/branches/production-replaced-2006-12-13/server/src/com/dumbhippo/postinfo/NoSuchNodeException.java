package com.dumbhippo.postinfo;

import java.util.Arrays;

/**
 * Requested node doesn't exist
 * 
 * @author hp
 */
public class NoSuchNodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public NoSuchNodeException(NodeName name) {
		super("No node " + name + " found");
	}
	
	public NoSuchNodeException(NodeName parent, NodeName... path) {
		super("Node " + parent + " does not have child " + Arrays.toString(path));
	}
}
