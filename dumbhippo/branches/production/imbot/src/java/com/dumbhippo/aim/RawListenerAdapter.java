package com.dumbhippo.aim;

public abstract class RawListenerAdapter implements RawListener {

	public void handleMessage(ScreenName buddy, String htmlMessage) throws FilterException {
	}
	
	public void handleChatMessage(ScreenName buddy, String chatRoomId, String htmlMessage) {
	}

	public void handleSetEvilAmount(ScreenName whoEviledUs, int amount) {
	}

	public void handleBuddySignOn(ScreenName buddy, String htmlInfo) {
	}

	public void handleBuddySignOff(ScreenName buddy, String htmlInfo) {
	}

	public void handleBuddyUnavailable(ScreenName buddy, String htmlMessage) {
	}

	public void handleBuddyAvailable(ScreenName buddy, String htmlMessage) {
	}

	public void handleUpdateBuddy(ScreenName buddy, String group) {
	}

	public void handleAddPermitted(ScreenName buddy) {
	}

	public void handleAddDenied(ScreenName buddy) {
	}

	public void handleConnected() {
	}

	public void handleDisconnected() {
	}

	public void handleError(TocError error, String message) {
	}

}
