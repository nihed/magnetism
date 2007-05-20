package com.dumbhippo.dm;

import java.io.Reader;
import java.io.StringReader;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchLexer;
import com.dumbhippo.dm.parser.FetchParser;

public class FetchBindingTests extends AbstractSupportedTests {
	protected void doTest(Class<? extends DMObject> clazz, String input, String expected) throws RecognitionException, TokenStreamException {
		Reader in = new StringReader(input);
		FetchParser parser = new FetchParser(new FetchLexer(in));
		FetchNode fetchNode = parser.startRule();
		Fetch fetch = fetchNode.bind(DataModel.getInstance().getDMClass(clazz));
		String output = fetch.toString();
		
		assertEquals(expected.replace("@", "http://mugshot.org/p/o/test"), output);
	}
	
	// Test basic operation of binding
	public void testBasic() throws Exception {
		doTest(TestGroupDMO.class, "name", "@/group#name");
		doTest(TestGroupDMO.class, "name;members", "@/group#members;@/group#name");
		doTest(TestGroupDMO.class, "name;members member", "@/group#members @/groupMember#member;@/group#name");
	}
	
	// Test '+' meaning 'defaults'
	public void testDefaults() throws Exception {
		doTest(TestGroupDMO.class, "+", "+");
		doTest(TestGroupDMO.class, "+;name", "+");
		doTest(TestGroupDMO.class, "+;members", "+;@/group#members");
		doTest(TestGroupDMO.class, "members;+", "+;@/group#members");
	}
	
	// TODO: additional tests for subclassing once that is implemented
}