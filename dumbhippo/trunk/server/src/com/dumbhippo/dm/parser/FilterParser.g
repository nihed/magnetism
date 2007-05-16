header { 
package com.dumbhippo.dm.parser;

import com.dumbhippo.dm.filter.*;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;
}

class FilterParser extends Parser;

options {
   k = 2;
}

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
}

startRule returns [Filter f = null]
    :   f=orExpression EOF
    ;
    
orExpression returns [Filter f = null]
{ Filter f2; }
	: f=andExpression ( OR f2=andExpression { f = new OrFilter(f, f2); } ) *
	;
	
andExpression returns [Filter f = null]
{ Filter f2; }
	: f=notExpression ( AND f2=notExpression { f = new AndFilter(f, f2); } ) *
	;
	
notExpression returns [Filter f = null]
{ Filter f1; }
	:   f=term
	  |	NOT f1=term { f = new NotFilter(f1); }
	;
	
term returns [Filter f = null]
{ FilterTermType type = null; }
	:	LPAREN f=orExpression RPAREN 
	  | "viewer" DOT pred:NAME LPAREN type=termType ( DOT prop:NAME )? RPAREN 
	    { f = new SimpleFilter(pred.getText(), type, prop != null ? prop.getText() : null); }
	;
	  
termType returns [FilterTermType t = null]
	: "key" { t = FilterTermType.KEY; }
	| "item" { t = FilterTermType.ITEM; }
	| "any" { t = FilterTermType.ANY; }
	| "all" { t = FilterTermType.ALL; }
	;

class FilterLexer extends Lexer ;

OR : "||" ;
AND : "&&" ;
NOT : "!" ;
LPAREN : "(" ;
RPAREN : ")" ;
DOT : "." ;
NAME : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')* ;
WS : ( ' ' | '\t' | '\r' | '\n' | '\f' ) { $setType(Token.SKIP); } ;
