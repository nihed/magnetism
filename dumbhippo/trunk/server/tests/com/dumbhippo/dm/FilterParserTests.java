package com.dumbhippo.dm;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.parser.FilterParser;

public class FilterParserTests extends AbstractParserTests {
	public String parse(String input) throws RecognitionException, TokenStreamException {
		return FilterParser.parse(input).toString();
	}
		
	public void testParser() throws Exception {
		expectIdentity("viewer.p(key)");
		expectIdentity("viewer.p(item)");
		expectIdentity("viewer.p(all)");
		expectIdentity("viewer.p(key.property)");
		expectSuccess("!viewer.p(key)", "!(viewer.p(key))");
		expectSuccess("viewer.p(key)||viewer.q(key)&&viewer.r(key)", "(viewer.p(key))||((viewer.q(key))&&(viewer.r(key)))");
		expectSuccess("viewer.p(key)&&viewer.q(key)||viewer.r(key)", "((viewer.p(key))&&(viewer.q(key)))||(viewer.r(key))");
		expectFailure("+");
		expectFailure("viewer.p()");
	}
}
