package com.dumbhippo.dm;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.parser.FilterLexer;
import com.dumbhippo.dm.parser.FilterParser;

public class FilterParserTests extends TestCase {
	private String parse(String input) throws RecognitionException, TokenStreamException {
		Reader in = new StringReader(input);
		FilterParser parser = new FilterParser(new FilterLexer(in));
		Filter filter = parser.startRule();
		
		return filter.toString();
	}
	
	private void expectSuccess(String input, String expectedOutput) throws RecognitionException, TokenStreamException {
		String output = parse(input);
		assertEquals(output, expectedOutput);
	}
	
	private void expectIdentity(String input) throws RecognitionException, TokenStreamException {
		expectSuccess(input, input);
	}

	private void expectFailure(String input) {
		boolean failed = false;
		
		try {
			parse(input);
		} catch (Exception e) {
			failed = true;
		}
		
		assertTrue(failed);
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
