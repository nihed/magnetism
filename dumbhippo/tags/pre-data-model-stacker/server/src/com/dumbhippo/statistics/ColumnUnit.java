package com.dumbhippo.statistics;

/**
 * This enumeration is used to represent the units of a column. This is principally
 * useful for labelling axes. In some cases (e.g. BYTES, MILLISECONDS), you'd seldom
 * want to display the raw value directly, but would instead display more user-friendly
 * larger units (megabytes, minutes, say.) 
 * @author otaylor
 */
public enum ColumnUnit {
	COUNT,  // a count of something 
	EVENTS, // number of events that have happened (like COUNT, but slightly more specific)
	BYTES,  // a memory value, in bytes
	MILLISECONDS // a time value, in milliseconds
}
