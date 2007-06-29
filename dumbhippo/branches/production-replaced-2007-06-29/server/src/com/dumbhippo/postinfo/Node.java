package com.dumbhippo.postinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is sort of like a DOM node, but no attributes, and can't mix
 * content and children (i.e. has either children or content, not both).
 * Also node names are our NodeName enum and not strings.
 * 
 * @author hp
 *
 */
public class Node {
	
	private NodeName name;
	
	// Invariants:
	// - 0 or 1 of content,children can be non-null, but not both
	// - the list can't be empty; it has to be null instead
	
	private String content;
	private List<Node> children;
	private boolean immutable;
	
	public Node(NodeName name) {
		setName(name);
	}
	
	public Node(NodeName name, String content) {
		setName(name);
		setContent(content);
	}
	
	public Node(NodeName name, List<Node> children) {
		setName(name);
		setChildren(children);
	}
	
	public Node(Node original) {
		// this deliberately doesn't copy the "immutable" flag since 
		// getting a mutable copy is the main point of copying
		setName(original.name);
		if (original.content != null)
			setContent(original.content);
		else if (original.children != null) {
			for (Node c : original.children)
				appendChild(new Node(c));	
		}
	}
	
	public void makeImmutable() {
		if (immutable)
			return; // no children could have been added, so no need to recurse
		immutable = true;
		if (children != null) {
			for (Node c : children)
				c.makeImmutable();
		}
	}
	
	/** 
	 * Returns true if the node is empty; which means it is indeterminate in 
	 * type, you can treat it as either content or container node.
	 * It will have content = all-whitespace string and children = empty list.
	 * @return true if the node is empty
	 */
	public boolean isEmpty() {
		return (children == null && content == null) || (children == null && content.trim().length() == 0);
	}
	
	/**
	 * Find out of if you can call getChildren(); node may still have 
	 * zero children if this returns true.
	 * 
	 * @return true if node can be treated as a container node
	 */
	public boolean hasChildren() {
		return children != null || isEmpty();
	}
	
	/**
	 * Find out if you can call getContent(); node may contain the 
	 * empty string though.
	 * @return if node can be treated as a content node
	 */
	public boolean hasContent() {
		return content != null || isEmpty();
	}
	
	public boolean hasChild(NodeName... path) {
		try {
			Node node = resolvePath(path);
			assert node != null;
			return true;
		} catch (NoSuchNodeException e) {
			return false;
		}
	}
	
	public List<Node> getChildren() {
		if (isEmpty()) {
			return Collections.emptyList();
		} else if (content != null) {
			throw new NodeContentException(name, "this is a text content node, no children");
		} else {
			return Collections.unmodifiableList(children);
		}
	}

	/**
	 * Sets the child nodes of the node, replacing 
	 * any existing content or child nodes. 
	 * Setting children to null deletes any existing child
	 * nodes or content.
	 * 
	 * @param children the new child nodes or null
	 */
	public void setChildren(List<Node> children) {
		if (immutable)
			throw new IllegalStateException("immutable node");
		if (children != null && !children.isEmpty())
			this.children = new ArrayList<Node>(children);	
		else
			this.children = null;
		this.content = null;
	}

	/**
	 * Not strictly needed since we have the varargs
	 * version, but maybe avoids an array creation or something. 
	 * @return the content (always non-null)
	 * @throws NodeContentException if node has children instead of content
	 */
	public String getContent() {
		if (content == null) {
			if (children != null)
				throw new NodeContentException(name, "no text content, this is a container node");
			else
				return "";
		}
		return content;
	}

	/**
	 * Sets the text content of the node, replacing any 
	 * existing content or existing child nodes.
	 * Setting content to null deletes both content and 
	 * children. Empty string is the same as null; it puts 
	 * the node in an indeterminate state where you can either 
	 * get the content (as empty string) or the children (as empty list)
	 * 
	 * @param content the new content or null
	 */
	public void setContent(String content) {
		if (immutable)
			throw new IllegalStateException("immutable node");
		if (content.length() == 0)
			content = null;
		this.content = content;
		this.children = null;
	}

	public NodeName getName() {
		return name;
	}

	/**
	 * Sets the name of the node; may not be null or NodeName.IGNORED
	 * @param name the new name of the node
	 */
	public void setName(NodeName name) {
		if (immutable)
			throw new IllegalStateException("immutable node");
		if (name == null || name == NodeName.IGNORED)
			throw new IllegalArgumentException("null or IGNORED node name");
		this.name = name;
	}
	
	public void appendChild(Node node) {
		if (immutable)
			throw new IllegalStateException("immutable node");
		if (this.children == null) {
			this.content = null; // children or content, not both
			this.children = new ArrayList<Node>();
		}
		this.children.add(node);
	}
	
	public Node getChild(int i) {
		if (this.children == null || i >= this.children.size())
			return null;
		else
			return this.children.get(i);
	}
	
	private Node resolveOrCreatePath(boolean create, int depth, NodeName... path) {
		if (create && immutable)
			throw new IllegalStateException("immutable node");
		Node current = this;
		if (depth < 0)
			depth = path.length;
		for (int i = 0; i < depth; ++i) {
			NodeName component = path[i];
			Node newCurrent = null;
			if (current.children != null) {
				for (Node child : current.children) {
					if (child.name == component) {
						newCurrent = child;
						break;
					}
				}
			}
			
			if (newCurrent != null) {
				current = newCurrent;
			} else if (create) {
				newCurrent = new Node(component);
				current.appendChild(newCurrent);
				current = newCurrent;
			} else {
				throw new NoSuchNodeException(this.name, path);
			}
		}
		return current;
	}
	
	/**
	 * A list of node names <i>not including this node</i>, 
	 * each name is the first child of the previous name
	 * found underneath this node. So e.g. if you have
	 * this node is &lt;PostInfo&gt; and it contains 
	 * &lt;Type&gt; you could resolvePath(NodeName.Type) to 
	 * get the &lt;Type&gt; node. With an empty path 
	 * (no arguments) just returns the node itself.
	 *  
	 * @param path list of child node names
	 * @return the child node
	 * @throws NoSuchNodeException if child node not found
	 */
	public Node resolvePath(NodeName... path) {
		return resolveOrCreatePath(false, -1, path);
	}
	
	public Node resolveOrCreatePath(NodeName... path) {
		return resolveOrCreatePath(true, -1, path);
	}
	
	public String getContent(NodeName... path) {
		Node node = resolvePath(path);
		return node.getContent();
	}
	
	public String getContentIfExists(NodeName... path) {
		try {
			return getContent(path);
		} catch (NoSuchNodeException e) {
			return null;
		}
	}

	public List<Node> getChildren(NodeName... path) {
		Node node = resolvePath(path);
		return node.getChildren();
	}

	public void removeChild(Node child) {
		if (immutable)
			throw new IllegalStateException("immutable node");
		int i = getChildren().indexOf(child);
		if (i < 0)
			throw new NoSuchNodeException(this.getName(), child.getName());
		children.remove(i);
	}
	
	public void removeChild(NodeName... path) {
		Node parent = resolveOrCreatePath(false, path.length - 1, path);
		Node child = parent.resolvePath(path[path.length - 1]);
		parent.removeChild(child);
	}
	
	public void removeChildIfExists(NodeName... path) {
		try {
			removeChild(path);
		} catch (NoSuchNodeException e) {
		}
	}
	
	public void removeChildIfNoChildren(NodeName... path) {
		Node parent = resolveOrCreatePath(false, path.length - 1, path);
		Node child = parent.resolvePath(path[path.length - 1]);
		if (child.isEmpty())
			parent.removeChild(child);
	}
	
	public void updateContentChild(String content, NodeName... path) {
		if (content != null)
			resolveOrCreatePath(path).setContent(content);
		else
			removeChildIfExists(path);
	}
	
	public void setBoolean(boolean value) {
		setContent(Boolean.toString(value));
	}
	
	public boolean getBoolean() {
		String s = getContent();
		if (s.equals("true"))
			return true;
		else if (s.equals("false"))
			return false;
		else
			throw new NodeContentException(name, "invalid boolean '" + s + "'");
	}
	
	public boolean getBoolean(NodeName... path) {
		Node node = resolvePath(path);
		return node.getBoolean();
	}

	public void setInteger(int value) {
		setContent(Integer.toString(value));
	}
	
	public int getInteger() {
		String s = getContent();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new NodeContentException(name, "Invalid integer '" + s + "'");
		}
	}
	
	public int getInteger(NodeName... path) {
		Node node = resolvePath(path);
		return node.getInteger();
	}
	
	@Override
	public boolean equals(Object obj) {
		// do not include the "immutable" flag
		
		if (this == obj)
			return true;
		if (!(obj instanceof Node))
			return false;
		Node other = (Node) obj;
		if (other.name != this.name)
			return false;
		if ((other.content != null) != (this.content != null))
			return false;
		if ((other.children != null) != (this.children != null))
			return false;
		if (other.content != null && !other.content.equals(this.content))
			return false;
		if (other.children != null) {
			if (other.children.size() != this.children.size())
				return false;
			for (int i = 0; i < other.children.size(); ++i) {
				if (!other.children.get(i).equals(this.children.get(i)))
					return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		// do not include the "immutable" flag
		
		// eh, this sucks
		int result = 17 + (name.ordinal() * (Integer.MAX_VALUE / NodeName.values().length));
		if (content != null) {
			result = 37 * result + content.hashCode();
		} else if (children != null) {
			// don't recurse through all the children, too slow
			result = 37 * result + children.size();
		}
		return result;
	}
	
	@Override
	public String toString() {
		return "<" + name.toString() + ">";
	}
}
