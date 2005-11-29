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
		Node root = new Node(NodeName.PostInfo);
		Node type = new Node(NodeName.Type, "GENERIC");
		root.appendChild(type);
		Node generic = new Node(NodeName.Generic);
		Node favicon = new Node(NodeName.Favicon, "http://example.com/favicon.ico");
		generic.appendChild(favicon);
		root.appendChild(generic);
		
		assertTrue(root.hasChildren());
		assertTrue(root.getChildren().size() == 2);
		assertEquals(root.resolvePath(NodeName.Type), type);
		assertTrue(root.hasChild(NodeName.Type));
		assertEquals(root.resolvePath(NodeName.Generic), generic);
		assertTrue(root.hasChild(NodeName.Generic));
		assertEquals(root.resolvePath(NodeName.Generic, NodeName.Favicon), favicon);
		assertTrue(root.hasChild(NodeName.Generic, NodeName.Favicon));
		
		// a node without content or children set is in an "indeterminate" state
		// where it can be treated as either a content node or container node
		Node empty = new Node(NodeName.PostInfo);
		assertTrue(empty.hasChildren());
		assertTrue(empty.hasContent());
		assertTrue(empty.getContent() != null);
		assertTrue(empty.getContent().length() == 0);
		assertTrue(empty.getChildren().size() == 0);
		
		// an all-whitespace node is similar but the content is the whitespace
		Node whitespace = new Node(NodeName.PostInfo, "   ");
		assertTrue(whitespace.hasChildren());
		assertTrue(whitespace.hasContent());
		assertTrue(whitespace.getContent() != null);
		assertTrue(whitespace.getContent().length() == 3);
		assertTrue(whitespace.getChildren().size() == 0);
	}
	
	public void testPostInfo() {
		String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		String document1 = xmlHeader
		+ "<PostInfo>\n" 
		+ "<Type>GENERIC</Type>\n"
		+ "<Generic>\n"
		+ "  <Favicon>http://example.com/favicon.ico</Favicon>\n"
		+ "</Generic>\n"
		+ "</PostInfo>\n";
		String document2 = xmlHeader
		+ "<PostInfo>\n" 
		+ "<Type>GENERIC</Type>\n"
		+ "<Generic>\n"
		+ "  <Favicon>  http://example.com/favicon.ico  </Favicon>\n"
		+ "</Generic>\n"
		+ "</PostInfo>\n";
		String document3 = xmlHeader
		+ "<PostInfo>\n" 
		+ "<Type>GENERIC</Type>\n"
		+ "<Generic>\n"
		+ "  <Favicon>    </Favicon>\n"
		+ "</Generic>\n"
		+ "</PostInfo>\n";
	
		PostInfo postInfo1 = parseSuccessfully(document1);
		
		assertTrue(postInfo1.getTree().hasChildren());
		assertTrue(postInfo1.getTree().hasChild(NodeName.Type));
		assertTrue(postInfo1.getTree().hasChild(NodeName.Generic));
		assertTrue(postInfo1.getTree().hasChild(NodeName.Generic, NodeName.Favicon));
		assertTrue(postInfo1.getTree().getChildren().size() == 2);
		
		assertEquals(postInfo1.getType(), PostInfoType.GENERIC);
		
		assertEquals(postInfo1.getContent(NodeName.Generic, NodeName.Favicon),
				"http://example.com/favicon.ico");
		
		PostInfo postInfo2 = parseSuccessfully(document2);
		assertEquals(postInfo2.getContent(NodeName.Generic, NodeName.Favicon),
		"  http://example.com/favicon.ico  ");

		PostInfo postInfo3 = parseSuccessfully(document3);
		assertEquals(postInfo3.getContent(NodeName.Generic, NodeName.Favicon),
		"    ");
		assertTrue(postInfo3.getTree().resolvePath(NodeName.Generic, NodeName.Favicon).hasChildren());
		assertTrue(postInfo3.getTree().resolvePath(NodeName.Generic, NodeName.Favicon).getChildren().size() == 0);
		
		String document3Rewritten = postInfo3.toXml();
		
		PostInfo postInfo4 = parseSuccessfully(document3Rewritten);
		
		assertEquals(postInfo4.getContent(NodeName.Generic, NodeName.Favicon),
		"    ");
		assertTrue(postInfo4.getTree().resolvePath(NodeName.Generic, NodeName.Favicon).hasChildren());
		assertTrue(postInfo4.getTree().resolvePath(NodeName.Generic, NodeName.Favicon).getChildren().size() == 0);
		
		assertEquals(postInfo3, postInfo4);
		
		TestUtils.testEqualsImplementation(postInfo1, postInfo2, postInfo3, postInfo4);
	}
}
