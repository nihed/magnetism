package com.dumbhippo.dm;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.parser.FilterParser;

public class FilterParserTests extends AbstractParserTests {
	@Override
	public String parse(String input) throws RecognitionException, TokenStreamException {
		return FilterParser.parse(input).toString();
	}
		
	public void testParser() throws Exception {
		expectIdentity("viewer.p(this)");
		expectIdentity("viewer.p(item)");
		expectIdentity("viewer.p(all)");
		expectIdentity("viewer.p(this.property)");
		expectSuccess("!viewer.p(this)", "!(viewer.p(this))");
		expectSuccess("viewer.p(this)||viewer.q(this)&&viewer.r(this)", "(viewer.p(this))||((viewer.q(this))&&(viewer.r(this)))");
		expectSuccess("viewer.p(this)&&viewer.q(this)||viewer.r(this)", "((viewer.p(this))&&(viewer.q(this)))||(viewer.r(this))");
		expectFailure("+");
		expectFailure("viewer.p()");
	}
}
