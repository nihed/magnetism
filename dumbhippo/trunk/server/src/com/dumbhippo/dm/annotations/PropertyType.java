package com.dumbhippo.dm.annotations;

public enum PropertyType {
	AUTO('X'),
	BOOLEAN('b'),
	INTEGER('i'),
	LONG('l'),
	FLOAT('f'),
	STRING('s'),
	RESOURCE('r'),
	FEED('f'),
	URL('u'),
	STORE_KEY('s'); // This one would not normally be sent over the wire; it's useful internally
	
	private char typeChar;

	PropertyType(char typeChar) {
		this.typeChar = typeChar;
	}
	
	public char getTypeChar() {
		return typeChar;
	}
}
