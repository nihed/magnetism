package com.dumbhippo.postinfo;

/**
 * Node doesn't have the requested content or child nodes
 * 
 * @author hp
 *
 */
public class NodeContentException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public NodeContentException(NodeName name) {
		super("Node " + name + " has invalid content");
	}

	public NodeContentException(NodeName name, String message) {
		super("Node " + name + " has invalid content: " + message);
	}

}
