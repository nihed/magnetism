package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.dm.parser.ParseException;

public class FetchParserTests extends AbstractParserTests {
	@Override
	protected String parse(String input) throws ParseException {
		return FetchParser.parse(input).toString();
	}
	
	public void testParser() throws Exception {
		expectIdentity("");
		
		expectIdentity("+");
		expectIdentity("name");
		expectIdentity("contact +");
		expectIdentity("contact [name;photoUrl];name");
		expectIdentity("name(notify=false) [name;photoUrl(notify=false)]");
		expectIdentity("blogEntries(max=10) [description;title]");
		expectIdentity("blogEntries(max=10,notify=false) [description;title]");
		expectIdentity("a;b [a;b [a;b]]");
		
		expectSuccess("member()[]", "member");
	
		// No trailing ";" allowed
		expectFailure("member;");
		expectFailure(";");
		
		// + can't have children
		expectFailure("+ name");
		expectFailure("+ [name]");
	}
	
	private void doMergeTest(String a, String b, String expected) throws ParseException {
		FetchNode fetchA = FetchParser.parse(a);
		FetchNode fetchB = FetchParser.parse(b);
		
		assertEquals(expected, fetchA.merge(fetchB).toString()); 
		assertEquals(expected, fetchB.merge(fetchA).toString()); 
	}
	
	public void testFetchMerge() throws Exception {
		doMergeTest("", "", "");
		doMergeTest("", "name", "name");
		doMergeTest("contact", "name", "contact;name");
		doMergeTest("blogEntries[description];contact", "blogEntries[title];name", "blogEntries [description;title];contact;name");
		doMergeTest("blogEntries(max=10)", "blogEntries(max=15,notify=false)", "blogEntries(max=15)");
		doMergeTest("blogEntries(max=10,notify=false)", "blogEntries(max=15,notify=false)", "blogEntries(max=15,notify=false)");
	}
}
