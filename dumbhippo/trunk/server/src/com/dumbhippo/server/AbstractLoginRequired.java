package com.dumbhippo.server;

import java.io.Serializable;

public class AbstractLoginRequired implements LoginRequired, Serializable {

	private static final long serialVersionUID = 0L;
	private String loggedInUserId;

	public String getLoggedInUserId() {
		return loggedInUserId;
	}

	public void setLoggedInUserId(String loggedInUserId) {
		this.loggedInUserId = loggedInUserId;
	}
}

