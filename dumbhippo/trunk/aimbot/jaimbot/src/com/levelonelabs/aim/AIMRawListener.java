package com.levelonelabs.aim;

public interface AIMRawListener extends AIMBaseListener {
    public void handleMessage(ScreenName buddy, String htmlMessage);

    public void handleSetEvilAmount(ScreenName whoEviledUs, int amount);

    public void handleBuddySignOn(ScreenName buddy, String htmlInfo);
    public void handleBuddySignOff(ScreenName buddy, String htmlInfo);

    public void handleBuddyUnavailable(ScreenName buddy, String htmlMessage);
    public void handleBuddyAvailable(ScreenName buddy, String htmlMessage);
}
