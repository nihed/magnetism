package com.dumbhippo.dm;

import junit.framework.TestCase;

import com.dumbhippo.dm.parser.ParseException;

public abstract class AbstractParserTests extends TestCase {
	protected abstract String parse(String input) throws ParseException;
	
	public void expectSuccess(String input, String expectedOutput) throws ParseException {
		String output = parse(input);
		assertEquals(expectedOutput, output);
	}
	
	public void expectIdentity(String input) throws ParseException {
		expectSuccess(input, input);
	}

	public void expectFailure(String input) {
		boolean failed = false;
		String output = null;
		
		try {
			output = parse(input);
		} catch (Exception e) {
			failed = true;
		}
		
		if (!failed)
			System.err.println("Print expected failure but got '" + output + "'");
		assertTrue(failed);
	}
}
