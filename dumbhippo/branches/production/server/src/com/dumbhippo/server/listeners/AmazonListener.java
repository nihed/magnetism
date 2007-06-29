package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.AmazonActivityStatus;

public interface AmazonListener {
    public void onAmazonActivityCreated(AmazonActivityStatus activityStatus);
}
