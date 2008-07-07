// $ANTLR : "FilterParser.g" -> "FilterParser.java"$
 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import com.dumbhippo.dm.filter.*;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class FilterParser extends antlr.LLkParser       implements FilterParserTokenTypes
 {

	// I'm not sure if reportError is ever called now that we've turned of the
	// defaultErrorHandler. But just in case...
	
	private static final Logger logger = GlobalSetup.getLogger(FilterParser.class);
	
	@Override
	public void reportError(RecognitionException e) {
		logger.debug("{}:{}: {}", new Object[] { e.getLine(), e.getColumn(), e.getMessage() });
	}
	
	@Override
	public void reportError(String error) {
		logger.debug(error);
	}
	
	@Override
	public void reportWarning(String warning) {
		logger.debug(warning);
	}
	
	public static Filter parse(String input) throws ParseException {
		try {
			StringReader in = new StringReader(input);
			FilterParser parser = new FilterParser(new FilterLexer(in));
			Filter f = parser.startRule();
			in.close();
		
			return f;
		} catch (RecognitionException e) {
			throw new ParseException(e);
		} catch (TokenStreamException e) {
			throw new ParseException(e);
		}		
	}

protected FilterParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public FilterParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected FilterParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public FilterParser(TokenStream lexer) {
  this(lexer,2);
}

public FilterParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final Filter  startRule() throws RecognitionException, TokenStreamException {
		Filter f;
		
		
		f=orExpression();
		match(Token.EOF_TYPE);
		return f;
	}
	
	public final Filter  orExpression() throws RecognitionException, TokenStreamException {
		Filter f;
		
		Filter f2;
		
		f=andExpression();
		{
		_loop4:
		do {
			if ((LA(1)==OR)) {
				match(OR);
				f2=andExpression();
				f = new OrFilter(f, f2);
			}
			else {
				break _loop4;
			}
			
		} while (true);
		}
		return f;
	}
	
	public final Filter  andExpression() throws RecognitionException, TokenStreamException {
		Filter f;
		
		Filter f2;
		
		f=notExpression();
		{
		_loop7:
		do {
			if ((LA(1)==AND)) {
				match(AND);
				f2=notExpression();
				f = new AndFilter(f, f2);
			}
			else {
				break _loop7;
			}
			
		} while (true);
		}
		return f;
	}
	
	public final Filter  notExpression() throws RecognitionException, TokenStreamException {
		Filter f;
		
		Filter f1;
		
		switch ( LA(1)) {
		case LPAREN:
		case LITERAL_viewer:
		case LITERAL_false:
		case LITERAL_true:
		{
			f=term();
			break;
		}
		case NOT:
		{
			match(NOT);
			f1=term();
			f = new NotFilter(f1);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return f;
	}
	
	public final Filter  term() throws RecognitionException, TokenStreamException {
		Filter f;
		
		Token  pred = null;
		Token  prop = null;
		ConditionType type;
		
		switch ( LA(1)) {
		case LPAREN:
		{
			match(LPAREN);
			f=orExpression();
			match(RPAREN);
			break;
		}
		case LITERAL_viewer:
		{
			match(LITERAL_viewer);
			match(DOT);
			pred = LT(1);
			match(NAME);
			match(LPAREN);
			type=conditionType();
			{
			switch ( LA(1)) {
			case DOT:
			{
				match(DOT);
				prop = LT(1);
				match(NAME);
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			f = new Condition(pred.getText(), type, prop != null ? prop.getText() : null);
			break;
		}
		case LITERAL_false:
		{
			match(LITERAL_false);
			f = FalseFilter.getInstance();
			break;
		}
		case LITERAL_true:
		{
			match(LITERAL_true);
			f = TrueFilter.getInstance();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return f;
	}
	
	public final ConditionType  conditionType() throws RecognitionException, TokenStreamException {
		ConditionType t;
		
		
		switch ( LA(1)) {
		case LITERAL_this:
		{
			match(LITERAL_this);
			t = ConditionType.THIS;
			break;
		}
		case LITERAL_item:
		{
			match(LITERAL_item);
			t = ConditionType.ITEM;
			break;
		}
		case LITERAL_any:
		{
			match(LITERAL_any);
			t = ConditionType.ANY;
			break;
		}
		case LITERAL_all:
		{
			match(LITERAL_all);
			t = ConditionType.ALL;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"OR",
		"AND",
		"NOT",
		"LPAREN",
		"RPAREN",
		"\"viewer\"",
		"DOT",
		"NAME",
		"\"false\"",
		"\"true\"",
		"\"this\"",
		"\"item\"",
		"\"any\"",
		"\"all\"",
		"WS"
	};
	
	
	}
