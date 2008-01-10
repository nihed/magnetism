header { 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import com.dumbhippo.dm.fetch.*;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;
}

class FetchParser extends Parser;

options {
   k = 2;
   defaultErrorHandler = false; // We want error to be immediately fatal
                                // this isn't a compiler where the user needs all the errors
}

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
}

startRule returns [FetchNode f]
	: f=fetchString EOF
	;

fetchString returns [FetchNode f]
{ List<PropertyFetchNode> props = new ArrayList<PropertyFetchNode>();
  PropertyFetchNode p; 
}
    : ( p=propertyFetch { props.add(p); } ( SEMICOLON p=propertyFetch { props.add(p); } )* )?
    { f = new FetchNode(props.toArray(new PropertyFetchNode[props.size()])); }
    ;
    
propertyFetch returns [PropertyFetchNode pf]
{ String p;
  FetchAttributeNode a;
  PropertyFetchNode childPf;
  List<FetchAttributeNode> attrs = Collections.emptyList();
  FetchNode children = null;
}
	: (
	    p=namedProperty ( attrs=attributes )?
	    (   
	      LBRACKET children=fetchString RBRACKET
	    | 
	      childPf=propertyFetch { children = new FetchNode(new PropertyFetchNode[] { childPf }); }
	    )?
      |
        p=specialProperty ( attrs=attributes )?
	  )
	  { pf = new PropertyFetchNode(p, 
	  	                           attrs.toArray(new FetchAttributeNode[attrs.size()]), 
	   	                           children != null && children.getProperties().length > 0 ? children : null); }
	;
     
attributes returns [ArrayList<FetchAttributeNode> attrs = new ArrayList<FetchAttributeNode>();]
{ FetchAttributeNode a; }
    : LPAREN ( a=attribute { attrs.add(a); } ( COMMA a=attribute { attrs.add(a); } ) * )? RPAREN
	;
        
namedProperty returns [String s]
	: n:NAME { s = n.getText(); }
	;
	
specialProperty returns [String s]
	:   PLUS { s = "+"; }
  	  | STAR { s = "*"; }
	;
        
attribute returns [FetchAttributeNode a]
{ FetchAttributeType t; 
  Object v; 
}
	: t=attributeType EQUALS (v=positiveInteger | v=bool) 
	  { a = new FetchAttributeNode(t, v); }
	;
	
attributeType returns [FetchAttributeType t]
	:  "max" { t = FetchAttributeType.MAX; }
	 | "notify" { t = FetchAttributeType.NOTIFY; }
	;	
	
positiveInteger returns [Integer i]
	: n:DIGITS { i = new Integer(n.getText()); }
	;

bool returns [Boolean b]
	:   "true" { b = true; }
	  | "false" { b = false; }
	;

class FetchLexer extends Lexer ;

options {
	defaultErrorHandler = false;
}

LBRACKET : "[" ;
RBRACKET : "]" ;
EQUALS : "=" ;
LPAREN : "(" ;
RPAREN : ")" ;
PLUS : "+" ;
COMMA : "," ;
SEMICOLON : ";" ;
STAR : "*" ;
NAME : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')* ;
DIGITS : ('0' .. '9')+ ;
WS : ( ' ' | '\t' | '\r' | '\n' | '\f' ) { $setType(Token.SKIP); } ;
