package com.dumbhippo.postinfo;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;

class PostInfoSaxHandler extends EnumSaxHandler<NodeName> {

	private Node tree;
	private List<Node> stack;
	private PostInfoType type;
	
	PostInfoSaxHandler() {
		super(NodeName.class, NodeName.IGNORED);
		stack = new ArrayList<Node>();
	}

	public Node getTree() {
		return tree;
	}
	
	public PostInfoType getPostInfoType() {
		return type;
	}
	
	@Override
	protected void openElement(NodeName element) throws SAXException {
		if (element == NodeName.IGNORED)
			return;
		
		Node node = new Node(element);
		if (!stack.isEmpty()) {
			Node parent = stack.get(stack.size() - 1);
			parent.appendChild(node);
		}
		stack.add(node);
		
		// drop content at start of each node since we never 
		// have mixed content and children
		clearContent();
	}
	
	@Override
	protected void closeElement(NodeName element) throws SAXException {
		if (element == NodeName.IGNORED)
			return;
		
		Node node = stack.get(stack.size() - 1);
		String content = getCurrentContent();

		// don't set content if we already have children
		if (node.isEmpty())
			node.setContent(content);

		stack.remove(stack.size() - 1);
		
		if (stack.isEmpty() && node.getName() == NodeName.postInfo)
			tree = node;
	
		// replace type if we found a more specific one; PostInfo can have 0-2 children,
		// where the two allowed children are <generic> and a more specific type
		if (parent() == NodeName.postInfo) {
			PostInfoType t = PostInfoType.fromNodeName(element);
			if (t == null) {
				throw new SAXException("Node " + element + " not allowed underneath " + parent());
			} else if (t == PostInfoType.GENERIC) {
				if (type == null) // don't override a more-specific type
					type = t;
			} else {
				// t is a specific type
				if (type == null || type == PostInfoType.GENERIC)
					type = t; // specific type wins over null or generic
				else
					throw new SAXException("Node " + element + " seen but we already had a type-specific node indicating type " + type);
			}
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		if (!stack.isEmpty())
			throw new SAXException("unbalanced tag " + stack.get(stack.size() - 1).getName());
		
		if (tree == null)
			throw new SAXException("no PostInfo root node found");
	
		if (type == null)
			type = PostInfoType.GENERIC; // no child nodes found
		
		// further validation depends on the type
	}
}
