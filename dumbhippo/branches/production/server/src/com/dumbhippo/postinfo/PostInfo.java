package com.dumbhippo.postinfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;

public class PostInfo {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PostInfo.class);
	
	static private SAXParserFactory saxFactory;
	
	static private SAXParser newSAXParser() {
		synchronized (PostInfo.class) {
			if (saxFactory == null)
				saxFactory = SAXParserFactory.newInstance();
		}
		try {
			return saxFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}	
	
	public static PostInfo parse(InputStream is) throws IOException, SAXException {
		SAXParser parser = newSAXParser();
		PostInfoSaxHandler handler = new PostInfoSaxHandler();
		parser.parse(is, handler);
		return newInstance(handler.getTree(), handler.getPostInfoType(), handler.getPostInfoType().getSubClass()); 
	}
	
	public static PostInfo parse(String s) throws SAXException {
		try {
			return parse(new ByteArrayInputStream(s.getBytes("UTF-8")));
		} catch (IOException e) {
			throw new RuntimeException("wtf, ioexception on ByteArrayInputStream", e);
		}
	}
	
	/**
	 * Create a new PostInfo of the appropriate subclass for this node tree.
	 * 
	 * @param tree a node tree which will now be owned by this PostInfo
	 * @param type the type of post info
	 * @param subClass this is just type.getSubClass(), it's a hack to pass it in
	 * @return a new PostInfo of the right subclass
	 * @throws IllegalArgumentException if the tree doesn't have a type node
	 */
	private static <T extends PostInfo> T newInstance(Node tree, PostInfoType type, Class<T> subClass) {
		if (tree == null)
			throw new IllegalArgumentException("null tree for PostInfo.newInstance");
		if (type == null)
			throw new IllegalArgumentException("null type for new PostInfo");
		if (!(type.getSubClass().equals(subClass)))
			throw new IllegalArgumentException("PostInfoType subclass does not match expected class");
		if (subClass.equals(PostInfo.class))
			throw new IllegalArgumentException("PostInfo can't be instantiated, make a subclass");
		
		T p;
		try {
			p = subClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		p.tree = tree;
		if (p.type != type)
			throw new RuntimeException("subclass of PostInfo " + subClass.getName() + " did not init type field to " + type);
		return p;
	}
	
	public static <T extends PostInfo> T newInstance(PostInfo original, PostInfoType newType, Class<T> subClass) {
		Node tree = new Node(original.getTree());
		if (original.getType() != PostInfoType.GENERIC && 
				original.getType() != newType) {
			// Drop data for the old type
			tree.removeChildIfExists(original.getType().getNodeName());
		}
		return newInstance(tree, newType, subClass);
	}
	
	public static <T extends PostInfo> T newInstance(PostInfoType type, Class<T> subClass) {
		Node root = new Node(NodeName.postInfo);
		root.appendChild(new Node(type.getNodeName()));
		return newInstance(root, type, subClass);
	}
	
	private Node tree;
	private PostInfoType type;
	
	/**
	 * Use newInstance() instead so you get the right subclass
	 * @param tree the node tree to initialize with or null to fill in postconstruct
	 * @param type the type of PostInfo
	 */
	protected PostInfo(Node tree, PostInfoType type) {
		this.tree = tree;
		this.type = type;
	}
	
	public PostInfoType getType() {
		return type;
	}
	
	public Node getTree() {
		return tree;
	}
	
	public void makeImmutable() {
		// there's no setter for "type"
		tree.makeImmutable();
	}
	
	/**
	 * lame convenience function, equivalent to getTree().getContent(path)
	 * @param path nested children, not including the PostInfo root node
	 * @return the content of the specified child
	 * @throws NoSuchNodeException if path doesn't exist
	 * @throws NodeContentException if node doesn't have text content
	 */
	public String getContent(NodeName... path) {
		return tree.getContent(path);
	}
	
	private void appendNodeXml(XmlBuilder xml, Node node, int depth) {
		xml.openElement(node.getName().name());
		// if the node has both content and children then it's 
		// empty/indeterminate and we want to be sure to write out
		// any whitespace content in case the whitespace is significant
		if (node.hasContent()) {
			xml.appendEscaped(node.getContent());
		} else if (node.hasChildren()) {
			for (Node c : node.getChildren()) {
				appendNodeXml(xml, c, depth + 1);
			}
		}
		xml.closeElement();
	}
	
	public String toXml() {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		appendNodeXml(xml, tree, 0);
		return xml.toString();
	}

	@Override
	public String toString() {
		// FIXME this is pretty expensive, should disable once stuff is working nicely
		return type + ": " + toXml().replace("\n", "");
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof PostInfo))
			return false;
		PostInfo other = (PostInfo) obj;
		if (other.type != this.type)
			return false;
		if ((other.tree != null) != (this.tree != null)) {
			return false;
		}
		if (other.tree != null && !other.tree.equals(this.tree))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result;
		if (tree == null)
			result = 17;
		else 
			result = tree.hashCode();
		return result + type.hashCode() * 37;
	}
}
