package com.dumbhippo.persistence;

/**
 * This enum goes in the database, so don't change values without migrating the db
 * 
 * @author Havoc Pennington
 *
 */
public enum Sentiment {
	INDIFFERENT,
	HATE,
	LOVE;
}
