package com.dumbhippo.statistics;

/**
 * Represents a sampling frequency. The idea here is that we typically record
 * data at the SECONDS_15 interval, but unless the user has drilled down to
 * a very small part of the data set, we generally don't want to return the
 * full data over the web interface, so we provide a number of larger granularity
 * timescales as well. Having a discrete set of timescales has a couple of 
 * advantages when compared to letting the requester specify arbitrary numbers
 * of seconds: first we set up timescales that are multiples of each other, so
 * we don't have to worry about a mismatch making it hard to aggregate data.
 * Second, there is more potential for caching aggregated data if the same scales
 * are asked for repeatedly. 
 * @author otaylor
 */
public enum Timescale {
	SECONDS_15(15),
	MINUTES_1(60),
	MINUTES_5(300),
	MINUTES_15(900),
	HOURS_1(3600);
	
	private int seconds;
	
	private Timescale(int seconds) {
		this.seconds = seconds;
	}
	
	static public Timescale get(int seconds) {
		if (seconds < 60)
			return SECONDS_15;
		else if (seconds < 300)
			return MINUTES_1;
		else if (seconds < 900)
			return MINUTES_5;
		else if (seconds < 3600)
			return MINUTES_15;
		else
			return HOURS_1;
	}
	
	public int getSeconds() {
		return seconds;
	}
}
