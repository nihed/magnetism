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

public interface FetchParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int SEMICOLON = 4;
	int LBRACKET = 5;
	int RBRACKET = 6;
	int LPAREN = 7;
	int COMMA = 8;
	int RPAREN = 9;
	int NAME = 10;
	int PLUS = 11;
	int STAR = 12;
	int EQUALS = 13;
	int LITERAL_max = 14;
	int LITERAL_notify = 15;
	int DIGITS = 16;
	int LITERAL_true = 17;
	int LITERAL_false = 18;
	int WS = 19;
}
