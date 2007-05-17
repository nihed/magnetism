header { 
package com.dumbhippo.dm.parser;

import java.util.List;
import java.util.ArrayList;
import com.dumbhippo.dm.fetch.*;
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
	// I'm not sure if reportError is ever called now that we've turned of the
	// defaultErrorHandler. But just in case...
	
	private static final Logger logger = GlobalSetup.getLogger(FetchParser.class);
	
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
}

startRule returns [Fetch f]
	: f=fetchString EOF
	;

fetchString returns [Fetch f]
{ List<PropertyFetch> props = new ArrayList<PropertyFetch>();
  PropertyFetch p; 
}
    : ( p=propertyFetch { props.add(p); } ( SEMICOLON p=propertyFetch { props.add(p); } )* )?
    { f = new Fetch(props.toArray(new PropertyFetch[props.size()])); }
    ;
    
propertyFetch returns [PropertyFetch pf]
{ String p;
  FetchAttribute a;
  List<FetchAttribute> attrs = new ArrayList<FetchAttribute>();
  Fetch children = null;
}
	: p=property
	  ( LPAREN ( a=attribute { attrs.add(a); } ( COMMA a=attribute { attrs.add(a); } ) * )? RPAREN )?
	  ( LBRACKET children=fetchString RBRACKET )?
	  { pf = new PropertyFetch(p, 
	  	                       attrs.toArray(new FetchAttribute[attrs.size()]), 
	  	                       children != null && children.getProperties().length > 0 ? children : null); }
	;
        
property returns [String s]
	:   PLUS { s = "+"; }
  	  | STAR { s = "*"; }
	  | n:NAME { s = n.getText(); }
	;
	
attribute returns [FetchAttribute a]
{ FetchAttributeType t; 
  Object v; 
}
	: t=attributeType EQUALS (v=positiveInteger | v=bool) 
	  { a = new FetchAttribute(t, v); }
	;
	
attributeType returns [FetchAttributeType t]
	:  "notify" { t = FetchAttributeType.NOTIFY; }
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
SEMICOLON : ";" ;
STAR : "*" ;
NAME : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')* ;
INTEGER : ('0' .. '9')+ ;
WS : ( ' ' | '\t' | '\r' | '\n' | '\f' ) { $setType(Token.SKIP); } ;
