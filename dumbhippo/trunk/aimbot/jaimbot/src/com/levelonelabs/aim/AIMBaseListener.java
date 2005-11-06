package com.levelonelabs.aim;

public interface AIMBaseListener {

	public void handleConnected();

	public void handleDisconnected();

	public void handleError(String error, String message);

}