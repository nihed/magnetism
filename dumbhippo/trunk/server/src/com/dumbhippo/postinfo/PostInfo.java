package com.dumbhippo.postinfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.dumbhippo.XmlBuilder;

public class PostInfo {
	
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
		return newInstance(handler.getTree()); 
	}
	
	public static PostInfo parse(String s) throws SAXException {
		try {
			return parse(new ByteArrayInputStream(s.getBytes("UTF-8")));
		} catch (IOException e) {
			throw new RuntimeException("wtf, ioexception on ByteArrayInputStream", e);
		}
	}
	
	private static PostInfoType parseType(String s) {
		PostInfoType t;
		try {
			t = Enum.valueOf(PostInfoType.class, s);
		} catch (IllegalArgumentException e) {
			// we can still read the generic portion
			t = PostInfoType.GENERIC;
		}
		return t;
	}
	
	/**
	 * Create a new PostInfo of the appropriate subclass for this node tree.
	 * 
	 * @param tree a node tree
	 * @return a new PostInfo of the right subclass
	 * @throws IllegalArgumentException if the tree doesn't have a type node
	 */
	public static PostInfo newInstance(Node tree) {
		if (tree == null)
			throw new IllegalArgumentException("null tree for PostInfo.newInstance");
		if (!tree.hasChild(NodeName.Type))
			throw new IllegalArgumentException("tree doesn't have a Type node");
		
		PostInfoType t = parseType(tree.getContent(NodeName.Type));
		// FIXME create an AmazonPostInfo, etc.
		
		return new PostInfo(tree);
	}
	
	private Node tree;
	
	/**
	 * Use newInstance() instead so you get the right subclass
	 * @param tree the node tree to initialize with
	 */
	protected PostInfo(Node tree) {
		this.tree = tree;
	}
	
	public PostInfoType getType() {
		String s = tree.getContent(NodeName.Type);
		return parseType(s);
	}
	
	public Node getTree() {
		return tree;
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
		if (node.hasChildren()) {
			for (Node c : node.getChildren()) {
				appendNodeXml(xml, c, depth + 1);
			}
		} else if (node.hasContent()) {
			xml.appendEscaped(node.getContent());
		}
		xml.closeElement();
	}
	
	public String toXml() {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		appendNodeXml(xml, tree, 0);
		return xml.toString();
	}
}
