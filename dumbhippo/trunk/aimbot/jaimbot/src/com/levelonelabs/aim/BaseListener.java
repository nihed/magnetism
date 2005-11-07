package com.levelonelabs.aim;

public interface BaseListener {

	public void handleConnected();

	public void handleDisconnected();

	public void handleError(String error, String message);

}