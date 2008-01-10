package com.dumbhippo.statistics;

/**
 * This enumeration describes the way that data is collected for a particular column
 * This largely affects the way we aggregate together column values when viewing
 * the data at a larger timescale. For a value that represents a total cumulative
 * count, averaging over the time doesn't make sense, but if the value represents
 * an instantaneous snapshot value, then an average might be sensible.
 **/
public enum ColumnType {
	SNAPSHOT, // The data represents an instantaneous value, such as the number
	          // of outstanding web page requests
	CUMULATIVE // the data represents a total amount of events that have happened since
	           // server startup, such as the number of web pages served
}
