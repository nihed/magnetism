package com.dumbhippo.dm;

import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.dm.parser.ParseException;

public class FilterParserTests extends AbstractParserTests {
	@Override
	public String parse(String input) throws ParseException {
		return FilterParser.parse(input).toString();
	}
		
	public void testParser() throws Exception {
		expectIdentity("true");
		expectIdentity("false");
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
