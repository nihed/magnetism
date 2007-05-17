package com.dumbhippo.dm;

import com.dumbhippo.identity20.Guid;

public class TestViewpoint implements DMViewpoint {
	private Guid viewerId;

	TestViewpoint(Guid viewerId) {
		this.viewerId = viewerId;
	}
	
	public Guid getViewerId() {
		return viewerId;
	}
}
