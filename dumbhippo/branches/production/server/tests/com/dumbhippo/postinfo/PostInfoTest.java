package com.dumbhippo.postinfo;

import org.xml.sax.SAXException;

import com.dumbhippo.server.TestUtils;

import junit.framework.TestCase;

public class PostInfoTest extends TestCase {

	private PostInfo parseSuccessfully(String s) {
		try {
			return PostInfo.parse(s);
		} catch (SAXException e) {
			throw new RuntimeException("Failure parsing PostInfo document", e);
		}
	}
	
	public void testNode() {
		Node root = new Node(NodeName.postInfo);
		Node generic = new Node(NodeName.generic);
		Node favicon = new Node(NodeName.favicon, "http://example.com/favicon.ico");
		generic.appendChild(favicon);
		root.appendChild(generic);
		
		assertTrue(root.hasChildren());
		assertTrue(root.getChildren().size() == 1);
		assertEquals(root.resolvePath(NodeName.generic), generic);
		assertTrue(root.hasChild(NodeName.generic));
		assertEquals(root.resolvePath(NodeName.generic, NodeName.favicon), favicon);
		assertTrue(root.hasChild(NodeName.generic, NodeName.favicon));
		
		// test copy constructor
		assertEquals(root, new Node(root));
		
		// a node without content or children set is in an "indeterminate" state
		// where it can be treated as either a content node or container node
		Node empty = new Node(NodeName.postInfo);
		assertTrue(empty.hasChildren());
		assertTrue(empty.hasContent());
		assertTrue(empty.getContent() != null);
		assertTrue(empty.getContent().length() == 0);
		assertTrue(empty.getChildren().size() == 0);
		
		assertTrue(!root.equals(empty));
		
		// an all-whitespace node is similar but the content is the whitespace
		Node whitespace = new Node(NodeName.postInfo, "   ");
		assertTrue(whitespace.hasChildren());
		assertTrue(whitespace.hasContent());
		assertTrue(whitespace.getContent() != null);
		assertTrue(whitespace.getContent().length() == 3);
		assertTrue(whitespace.getChildren().size() == 0);
		
		assertTrue(!root.equals(whitespace));
		assertTrue(!empty.equals(whitespace));
	}
	
	public void testPostInfo() {
		String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		String document1 = xmlHeader
		+ "<postInfo>\n" 
		+ "<generic>\n"
		+ "  <favicon>http://example.com/favicon.ico</favicon>\n"
		+ "</generic>\n"
		+ "</postInfo>\n";
		String document2 = xmlHeader
		+ "<postInfo>\n" 
		+ "<generic>\n"
		+ "  <favicon>  http://example.com/favicon.ico  </favicon>\n"
		+ "</generic>\n"
		+ "</postInfo>\n";
		String document3 = xmlHeader
		+ "<postInfo>\n"
		+ "<generic>\n"
		+ "  <favicon>    </favicon>\n"
		+ "</generic>\n"
		+ "</postInfo>\n";
		String document4 = xmlHeader
		+ "<postInfo>\n" 
		+ "<generic>\n"
		+ "  <favicon>http://example.com/favicon.ico</favicon>\n"
		+ "</generic>\n"
		+ "<amazon>\n"
		+ "  <smallPhoto><url>http://example.com/foo.png</url><width>43</width><height>47</height></smallPhoto>\n"
		+ "  <newPrice>$45.00</newPrice>\n"
		+ "  <usedPrice>$46.00</usedPrice>\n"
		+ "  <refurbishedPrice>$47.00</refurbishedPrice>\n"
		+ "  <collectiblePrice>$48.00</collectiblePrice>\n"
		+ "</amazon>\n"
		+ "</postInfo>\n";
		String document5 = xmlHeader
		+ "<postInfo>\n" 
		+ "<generic>\n"
		+ "  <favicon>http://example.com/favicon.ico</favicon>\n"
		+ "</generic>\n"
		+ "<eBay>\n"
		+ "  <smallPhoto><url>http://example.com/foo.png</url></smallPhoto>\n"
		+ "  <startPrice>$30.00</startPrice>\n"
		+ "  <buyItNowPrice>$36.00</buyItNowPrice>\n"
		+ "</eBay>\n"
		+ "</postInfo>\n";
	
		
		PostInfo postInfo1 = parseSuccessfully(document1);
		
		assertTrue(postInfo1.getTree().hasChildren());
		assertTrue(postInfo1.getTree().hasChild(NodeName.generic));
		assertTrue(postInfo1.getTree().hasChild(NodeName.generic, NodeName.favicon));
		assertTrue(postInfo1.getTree().getChildren().size() == 1);
		
		assertEquals(postInfo1.getType(), PostInfoType.GENERIC);
		
		assertEquals(postInfo1.getContent(NodeName.generic, NodeName.favicon),
				"http://example.com/favicon.ico");
		
		PostInfo postInfo2 = parseSuccessfully(document2);
		assertEquals(postInfo2.getContent(NodeName.generic, NodeName.favicon),
		"  http://example.com/favicon.ico  ");

		assertTrue(!postInfo1.equals(postInfo2));
		
		PostInfo postInfo3 = parseSuccessfully(document3);
		assertEquals(postInfo3.getContent(NodeName.generic, NodeName.favicon),
		"    ");
		assertTrue(postInfo3.getTree().resolvePath(NodeName.generic, NodeName.favicon).hasChildren());
		assertTrue(postInfo3.getTree().resolvePath(NodeName.generic, NodeName.favicon).getChildren().size() == 0);
		
		String document3Rewritten = postInfo3.toXml();
		
		PostInfo postInfo3r = parseSuccessfully(document3Rewritten);
		
		assertEquals(postInfo3r.getContent(NodeName.generic, NodeName.favicon),
		"    ");
		assertTrue(postInfo3r.getTree().resolvePath(NodeName.generic, NodeName.favicon).hasChildren());
		assertTrue(postInfo3r.getTree().resolvePath(NodeName.generic, NodeName.favicon).getChildren().size() == 0);
		
		assertEquals(postInfo3, postInfo3r);

		PostInfo postInfo4 = parseSuccessfully(document4);
		assertTrue(postInfo4 instanceof AmazonPostInfo);
		assertTrue(postInfo4.getTree().getChildren().size() == 2);
		
		assertTrue(!postInfo1.equals(postInfo4));
		
		PostInfo postInfo5 = parseSuccessfully(document5);
		assertTrue(postInfo5 instanceof EbayPostInfo);
		assertTrue(postInfo5.getTree().getChildren().size() == 2);
		
		assertTrue(!postInfo1.equals(postInfo5));
		assertTrue(!postInfo4.equals(postInfo5));
		
		TestUtils.testEqualsImplementation(postInfo1, postInfo2, postInfo3, postInfo3r, postInfo4, postInfo5);
		// test Node copy constructor
		assertEquals(postInfo1.getTree(), new Node(postInfo1.getTree()));
		assertEquals(postInfo4.getTree(), new Node(postInfo4.getTree()));
		assertEquals(postInfo5.getTree(), new Node(postInfo5.getTree()));
	}
}
