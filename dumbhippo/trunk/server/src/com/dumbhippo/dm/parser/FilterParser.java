// $ANTLR : "FilterParser.g" -> "FilterParser.java"$
 
package com.dumbhippo.dm.parser;

import com.dumbhippo.dm.filter.*;
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
		Filter f = null;
		
		
		try {      // for error handling
			f=orExpression();
			match(Token.EOF_TYPE);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_0);
		}
		return f;
	}
	
	public final Filter  orExpression() throws RecognitionException, TokenStreamException {
		Filter f = null;
		
		Filter f2;
		
		try {      // for error handling
			f=andExpression();
			{
			_loop1062:
			do {
				if ((LA(1)==OR)) {
					match(OR);
					f2=andExpression();
					f = new OrFilter(f, f2);
				}
				else {
					break _loop1062;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_1);
		}
		return f;
	}
	
	public final Filter  andExpression() throws RecognitionException, TokenStreamException {
		Filter f = null;
		
		Filter f2;
		
		try {      // for error handling
			f=notExpression();
			{
			_loop1065:
			do {
				if ((LA(1)==AND)) {
					match(AND);
					f2=notExpression();
					f = new AndFilter(f, f2);
				}
				else {
					break _loop1065;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_2);
		}
		return f;
	}
	
	public final Filter  notExpression() throws RecognitionException, TokenStreamException {
		Filter f = null;
		
		Filter f1;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LPAREN:
			case LITERAL_viewer:
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
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
		return f;
	}
	
	public final Filter  term() throws RecognitionException, TokenStreamException {
		Filter f = null;
		
		Token  pred = null;
		Token  prop = null;
		FilterTermType type = null;
		
		try {      // for error handling
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
				type=termType();
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
				f = new SimpleFilter(pred.getText(), type, prop != null ? prop.getText() : null);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_3);
		}
		return f;
	}
	
	public final FilterTermType  termType() throws RecognitionException, TokenStreamException {
		FilterTermType t = null;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_key:
			{
				match(LITERAL_key);
				t = FilterTermType.KEY;
				break;
			}
			case LITERAL_item:
			{
				match(LITERAL_item);
				t = FilterTermType.ITEM;
				break;
			}
			case LITERAL_any:
			{
				match(LITERAL_any);
				t = FilterTermType.ANY;
				break;
			}
			case LITERAL_all:
			{
				match(LITERAL_all);
				t = FilterTermType.ALL;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			recover(ex,_tokenSet_4);
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
		"\"key\"",
		"\"item\"",
		"\"any\"",
		"\"all\"",
		"WS"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 258L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 274L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 306L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 1280L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	
	}
