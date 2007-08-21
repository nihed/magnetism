package com.dumbhippo.persistence;

public enum SubscriptionStatus {
	NONE(false, false), /* No subscription */
	FROM(false, true),  /* We send presence information to the remote XMPP resource */
	TO(true, false),    /* We get presence information from the remote XMPP resource (subscribed-to remote resource) */
	BOTH(true, true);   /* Both of the above */

	private boolean subscribedTo;
	private boolean subscribedFrom;
		
	SubscriptionStatus(boolean subscribedTo, boolean subscribedFrom) {
		this.subscribedTo = subscribedTo;
		this.subscribedFrom = subscribedFrom;
	}

	public boolean isSubscribedFrom() {
		return subscribedFrom;
	}

	public boolean isSubscribedTo() {
		return subscribedTo;
	}
}
