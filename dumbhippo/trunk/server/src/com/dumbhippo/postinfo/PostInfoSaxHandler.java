package com.dumbhippo.postinfo;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;

class PostInfoSaxHandler extends EnumSaxHandler<NodeName> {

	private Node tree;
	private List<Node> stack;
	
	PostInfoSaxHandler() {
		super(NodeName.class, NodeName.IGNORED);
		stack = new ArrayList<Node>();
	}

	public Node getTree() {
		return tree;
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
		// note that whitespace is significant for content nodes
		// but gets dropped for container nodes
		if (content.trim().length() > 0) {
			if (!node.isEmpty() && node.hasChildren())
				throw new SAXException("node " + node.getName() + " has both text content and child nodes");
			node.setContent(content);
		}
		stack.remove(stack.size() - 1);
		
		if (stack.isEmpty() && node.getName() == NodeName.PostInfo)
			tree = node;
	}
	
	@Override 
	public void endDocument() throws SAXException {
		if (!stack.isEmpty())
			throw new SAXException("unbalanced tag " + stack.get(stack.size() - 1).getName());
		
		if (tree == null)
			throw new SAXException("no PostInfo root node found");

		if (!tree.hasChildren())
			throw new SAXException("PostInfo does not have any child nodes");
		
		if (!tree.hasChild(NodeName.Type))
			throw new SAXException("PostInfo has no Type child node");
		
		// further validation depends on the type
	}
}
