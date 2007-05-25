package com.dumbhippo.dm;

import java.io.Reader;
import java.io.StringReader;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchLexer;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.identity20.Guid;

public class FetchBindingTests extends AbstractSupportedTests {
	protected <K, T extends DMObject<K>> void doTest(Class<K> keyClass, Class<T> objectClass, String input, String expected) throws RecognitionException, TokenStreamException {
		Reader in = new StringReader(input);
		FetchParser parser = new FetchParser(new FetchLexer(in));
		FetchNode fetchNode = parser.startRule();
		String output = fetchNode.bind(DataModel.getInstance().getClassHolder(keyClass, objectClass)).toString();
		
		assertEquals(expected.replace("@", "http://mugshot.org/p/o/test"), output);
	}
	
	// Test basic operation of binding
	public void testBasic() throws Exception {
		doTest(Guid.class, TestGroupDMO.class, "name", "@/group#name");
		doTest(Guid.class, TestGroupDMO.class, "name;members", "@/group#members;@/group#name");
		doTest(Guid.class, TestGroupDMO.class, "name;members member", "@/group#members @/groupMember#member;@/group#name");
	}
	
	// Test '+' meaning 'defaults'
	public void testDefaults() throws Exception {
		doTest(Guid.class, TestGroupDMO.class, "+", "+");
		doTest(Guid.class, TestGroupDMO.class, "+;name", "+");
		doTest(Guid.class, TestGroupDMO.class, "+;members", "+;@/group#members");
		doTest(Guid.class, TestGroupDMO.class, "members;+", "+;@/group#members");
	}
	
	// TODO: additional tests for subclassing once that is implemented
}