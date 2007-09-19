// $ANTLR : "FilterParser.g" -> "FilterParser.java"$
 
package com.dumbhippo.dm.parser;

import java.io.StringReader;
import com.dumbhippo.dm.filter.*;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;

public interface FilterParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int OR = 4;
	int AND = 5;
	int NOT = 6;
	int LPAREN = 7;
	int RPAREN = 8;
	int LITERAL_viewer = 9;
	int DOT = 10;
	int NAME = 11;
	int LITERAL_false = 12;
	int LITERAL_true = 13;
	int LITERAL_this = 14;
	int LITERAL_item = 15;
	int LITERAL_any = 16;
	int LITERAL_all = 17;
	int WS = 18;
}
