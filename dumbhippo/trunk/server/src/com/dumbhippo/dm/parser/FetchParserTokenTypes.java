// $ANTLR : "FetchParser.g" -> "FetchParser.java"$
 
package com.dumbhippo.dm.parser;

import java.util.List;
import java.util.ArrayList;
import com.dumbhippo.dm.fetch.*;
import com.dumbhippo.GlobalSetup;
import org.slf4j.Logger;

public interface FetchParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int SEMICOLON = 4;
	int LPAREN = 5;
	int COMMA = 6;
	int RPAREN = 7;
	int LBRACKET = 8;
	int RBRACKET = 9;
	int PLUS = 10;
	int STAR = 11;
	int NAME = 12;
	int EQUALS = 13;
	int LITERAL_notify = 14;
	int DIGITS = 15;
	int LITERAL_true = 16;
	int LITERAL_false = 17;
	int INTEGER = 18;
	int WS = 19;
}
