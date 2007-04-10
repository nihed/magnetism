package com.dumbhippo.polling;

public class PollResult {
	public boolean executionChanged;
	public boolean obsolete;
	public PollResult(boolean executionChanged, boolean obsolete) {
		this.executionChanged = executionChanged;
		this.obsolete = obsolete;
	}
}