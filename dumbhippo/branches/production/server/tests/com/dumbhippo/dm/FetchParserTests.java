package com.dumbhippo.dm;

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
		expectIdentity("name;contact [name;photoUrl]");
		expectIdentity("name(notify=false) [name;photoUrl(notify=false)]");
		expectIdentity("a;b [a;b [a;b]]");
		
		expectSuccess("member()[]", "member");
	
		// No trailing ";" allowed
		expectFailure("member;");
		expectFailure(";");
		
		// + can't have children
		expectFailure("+ name");
		expectFailure("+ [name]");
	}
}
