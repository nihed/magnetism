package com.levelonelabs.aim;

public interface AIMRawListener extends AIMBaseListener {
    public void handleMessage(String buddy, String htmlMessage);

    public void handleSetEvilAmount(String buddy, int amount);

    public void handleBuddySignOn(String buddy, String htmlInfo);
    public void handleBuddySignOff(String buddy, String htmlInfo);

    public void handleBuddyUnavailable(String buddy, String htmlMessage);
    public void handleBuddyAvailable(String buddy, String htmlMessage);
}
