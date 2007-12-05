// $ANTLR : "FetchParser.g" -> "FetchParser.java"$
 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import com.dumbhippo.dm.fetch.*;
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
	
	public static FetchNode parse(String input) throws ParseException {
		try {
   		    StringReader in = new StringReader(input);
		    FetchParser parser = new FetchParser(new FetchLexer(in));
	        FetchNode fetchNode = parser.startRule();
		    in.close();
		
			return fetchNode;
		} catch (RecognitionException e) {
			throw new ParseException(e);
		} catch (TokenStreamException e) {
			throw new ParseException(e);
		}
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
		case NAME:
		case PLUS:
		case STAR:
		{
			p=propertyFetch();
			props.add(p);
			{
			_loop5:
			do {
				if ((LA(1)==SEMICOLON)) {
					match(SEMICOLON);
					p=propertyFetch();
					props.add(p);
				}
				else {
					break _loop5;
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
		FetchAttributeNode a;
		PropertyFetchNode childPf;
		List<FetchAttributeNode> attrs = Collections.emptyList();
		FetchNode children = null;
		
		
		{
		switch ( LA(1)) {
		case NAME:
		{
			p=namedProperty();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				attrs=attributes();
				break;
			}
			case EOF:
			case SEMICOLON:
			case LBRACKET:
			case RBRACKET:
			case NAME:
			case PLUS:
			case STAR:
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
			case LBRACKET:
			{
				match(LBRACKET);
				children=fetchString();
				match(RBRACKET);
				break;
			}
			case NAME:
			case PLUS:
			case STAR:
			{
				childPf=propertyFetch();
				children = new FetchNode(new PropertyFetchNode[] { childPf });
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
		{
			p=specialProperty();
			{
			switch ( LA(1)) {
			case LPAREN:
			{
				attrs=attributes();
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
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		pf = new PropertyFetchNode(p, 
			  	                           attrs.toArray(new FetchAttributeNode[attrs.size()]), 
			   	                           children != null && children.getProperties().length > 0 ? children : null);
		return pf;
	}
	
	public final String  namedProperty() throws RecognitionException, TokenStreamException {
		String s;
		
		Token  n = null;
		
		n = LT(1);
		match(NAME);
		s = n.getText();
		return s;
	}
	
	public final ArrayList<FetchAttributeNode>  attributes() throws RecognitionException, TokenStreamException {
		ArrayList<FetchAttributeNode> attrs = new ArrayList<FetchAttributeNode>();;
		
		FetchAttributeNode a;
		
		match(LPAREN);
		{
		switch ( LA(1)) {
		case LITERAL_max:
		case LITERAL_notify:
		{
			a=attribute();
			attrs.add(a);
			{
			_loop14:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					a=attribute();
					attrs.add(a);
				}
				else {
					break _loop14;
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
		return attrs;
	}
	
	public final String  specialProperty() throws RecognitionException, TokenStreamException {
		String s;
		
		
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
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return s;
	}
	
	public final FetchAttributeNode  attribute() throws RecognitionException, TokenStreamException {
		FetchAttributeNode a;
		
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
		a = new FetchAttributeNode(t, v);
		return a;
	}
	
	public final FetchAttributeType  attributeType() throws RecognitionException, TokenStreamException {
		FetchAttributeType t;
		
		
		switch ( LA(1)) {
		case LITERAL_max:
		{
			match(LITERAL_max);
			t = FetchAttributeType.MAX;
			break;
		}
		case LITERAL_notify:
		{
			match(LITERAL_notify);
			t = FetchAttributeType.NOTIFY;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
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
		"LBRACKET",
		"RBRACKET",
		"LPAREN",
		"COMMA",
		"RPAREN",
		"NAME",
		"PLUS",
		"STAR",
		"EQUALS",
		"\"max\"",
		"\"notify\"",
		"DIGITS",
		"\"true\"",
		"\"false\"",
		"WS"
	};
	
	
	}
