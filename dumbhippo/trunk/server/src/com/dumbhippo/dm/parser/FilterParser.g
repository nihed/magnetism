header { 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import com.dumbhippo.dm.filter.*;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;
}

class FilterParser extends Parser;

options {
   k = 2;
   defaultErrorHandler = false; // We want error to be immediately fatal
                                // this isn't a compiler where the user needs all the errors
}

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
}

startRule returns [Filter f]
    :   f=orExpression EOF
    ;
    
orExpression returns [Filter f]
{ Filter f2; }
	: f=andExpression ( OR f2=andExpression { f = new OrFilter(f, f2); } ) *
	;
	
andExpression returns [Filter f]
{ Filter f2; }
	: f=notExpression ( AND f2=notExpression { f = new AndFilter(f, f2); } ) *
	;
	
notExpression returns [Filter f]
{ Filter f1; }
	:   f=term
	  |	NOT f1=term { f = new NotFilter(f1); }
	;
	
term returns [Filter f]
{ ConditionType type; }
	:	LPAREN f=orExpression RPAREN 
	  | "viewer" DOT pred:NAME LPAREN type=conditionType ( DOT prop:NAME )? RPAREN 
	    { f = new Condition(pred.getText(), type, prop != null ? prop.getText() : null); }
	;
	  
conditionType returns [ConditionType t]
	: "this" { t = ConditionType.THIS; }
	| "item" { t = ConditionType.ITEM; }
	| "any" { t = ConditionType.ANY; }
	| "all" { t = ConditionType.ALL; }
	;

class FilterLexer extends Lexer ;

options {
   defaultErrorHandler = false;
}

OR : "||" ;
AND : "&&" ;
NOT : "!" ;
LPAREN : "(" ;
RPAREN : ")" ;
DOT : "." ;
NAME : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')* ;
WS : ( ' ' | '\t' | '\r' | '\n' | '\f' ) { $setType(Token.SKIP); } ;
