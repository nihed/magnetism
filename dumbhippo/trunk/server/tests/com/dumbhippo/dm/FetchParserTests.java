package com.dumbhippo.dm;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.parser.FetchParser;

public class FetchParserTests extends AbstractParserTests {
	protected String parse(String input) throws RecognitionException, TokenStreamException {
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
