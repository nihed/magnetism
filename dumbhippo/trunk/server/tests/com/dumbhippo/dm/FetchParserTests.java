package com.dumbhippo.dm;

import java.io.Reader;
import java.io.StringReader;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchLexer;
import com.dumbhippo.dm.parser.FetchParser;

public class FetchParserTests extends AbstractParserTests {
	protected String parse(String input) throws RecognitionException, TokenStreamException {
		Reader in = new StringReader(input);
		FetchParser parser = new FetchParser(new FetchLexer(in));
		FetchNode filter = parser.startRule();
		
		return filter.toString();
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
		
		expectFailure("member;");
		expectFailure(";");
	}
}
