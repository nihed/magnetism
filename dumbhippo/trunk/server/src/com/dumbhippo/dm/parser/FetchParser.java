// $ANTLR : "FetchParser.g" -> "FetchParser.java"$
 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import com.dumbhippo.dm.fetch.*;
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

public class FetchParser extends antlr.LLkParser       implements FetchParserTokenTypes
 {

	private static final Logger logger = GlobalSetup.getLogger(FetchParser.class);
	
	// I'm not sure if reportError is ever called now that we've turned of the
	// defaultErrorHandler. But just in case...
	
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
	
	public static FetchNode parse(String input) throws RecognitionException, TokenStreamException {
		StringReader in = new StringReader(input);
		FetchParser parser = new FetchParser(new FetchLexer(in));
		FetchNode fetchNode = parser.startRule();
		in.close();
		
		return fetchNode;
	}

protected FetchParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public FetchParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected FetchParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public FetchParser(TokenStream lexer) {
  this(lexer,2);
}

public FetchParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final FetchNode  startRule() throws RecognitionException, TokenStreamException {
		FetchNode f;
		
		
		f=fetchString();
		match(Token.EOF_TYPE);
		return f;
	}
	
	public final FetchNode  fetchString() throws RecognitionException, TokenStreamException {
		FetchNode f;
		
		List<PropertyFetchNode> props = new ArrayList<PropertyFetchNode>();
		PropertyFetchNode p; 
		
		
		{
		switch ( LA(1)) {
		case PLUS:
		case STAR:
		case NAME:
		{
			p=propertyFetch();
			props.add(p);
			{
			_loop353:
			do {
				if ((LA(1)==SEMICOLON)) {
					match(SEMICOLON);
					p=propertyFetch();
					props.add(p);
				}
				else {
					break _loop353;
				}
				
			} while (true);
			}
			break;
		}
		case EOF:
		case RBRACKET:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		f = new FetchNode(props.toArray(new PropertyFetchNode[props.size()]));
		return f;
	}
	
	public final PropertyFetchNode  propertyFetch() throws RecognitionException, TokenStreamException {
		PropertyFetchNode pf;
		
		String p;
		FetchAttribute a;
		PropertyFetchNode childPf;
		List<FetchAttribute> attrs = new ArrayList<FetchAttribute>();
		FetchNode children = null;
		
		
		p=property();
		{
		switch ( LA(1)) {
		case LPAREN:
		{
			match(LPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_notify:
			{
				a=attribute();
				attrs.add(a);
				{
				_loop358:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						a=attribute();
						attrs.add(a);
					}
					else {
						break _loop358;
					}
					
				} while (true);
				}
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
			break;
		}
		case EOF:
		case SEMICOLON:
		case LBRACKET:
		case RBRACKET:
		case PLUS:
		case STAR:
		case NAME:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case EOF:
		case SEMICOLON:
		case LBRACKET:
		case RBRACKET:
		{
			{
			switch ( LA(1)) {
			case LBRACKET:
			{
				match(LBRACKET);
				children=fetchString();
				match(RBRACKET);
				break;
			}
			case EOF:
			case SEMICOLON:
			case RBRACKET:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case PLUS:
		case STAR:
		case NAME:
		{
			childPf=propertyFetch();
			children = new FetchNode(new PropertyFetchNode[] { childPf });
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		pf = new PropertyFetchNode(p, 
			  	                           attrs.toArray(new FetchAttribute[attrs.size()]), 
			  	                           children != null && children.getProperties().length > 0 ? children : null);
		return pf;
	}
	
	public final String  property() throws RecognitionException, TokenStreamException {
		String s;
		
		Token  n = null;
		
		switch ( LA(1)) {
		case PLUS:
		{
			match(PLUS);
			s = "+";
			break;
		}
		case STAR:
		{
			match(STAR);
			s = "*";
			break;
		}
		case NAME:
		{
			n = LT(1);
			match(NAME);
			s = n.getText();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return s;
	}
	
	public final FetchAttribute  attribute() throws RecognitionException, TokenStreamException {
		FetchAttribute a;
		
		FetchAttributeType t; 
		Object v; 
		
		
		t=attributeType();
		match(EQUALS);
		{
		switch ( LA(1)) {
		case DIGITS:
		{
			v=positiveInteger();
			break;
		}
		case LITERAL_true:
		case LITERAL_false:
		{
			v=bool();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		a = new FetchAttribute(t, v);
		return a;
	}
	
	public final FetchAttributeType  attributeType() throws RecognitionException, TokenStreamException {
		FetchAttributeType t;
		
		
		match(LITERAL_notify);
		t = FetchAttributeType.NOTIFY;
		return t;
	}
	
	public final Integer  positiveInteger() throws RecognitionException, TokenStreamException {
		Integer i;
		
		Token  n = null;
		
		n = LT(1);
		match(DIGITS);
		i = new Integer(n.getText());
		return i;
	}
	
	public final Boolean  bool() throws RecognitionException, TokenStreamException {
		Boolean b;
		
		
		switch ( LA(1)) {
		case LITERAL_true:
		{
			match(LITERAL_true);
			b = true;
			break;
		}
		case LITERAL_false:
		{
			match(LITERAL_false);
			b = false;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return b;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"SEMICOLON",
		"LPAREN",
		"COMMA",
		"RPAREN",
		"LBRACKET",
		"RBRACKET",
		"PLUS",
		"STAR",
		"NAME",
		"EQUALS",
		"\"notify\"",
		"DIGITS",
		"\"true\"",
		"\"false\"",
		"INTEGER",
		"WS"
	};
	
	
	}
