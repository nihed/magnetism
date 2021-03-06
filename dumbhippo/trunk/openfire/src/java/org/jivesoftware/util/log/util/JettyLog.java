/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.util.log.util;

import org.mortbay.log.Logger;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;

/**
 * A Logger implementation to override the default Jetty logging behavior. All log statements
 * are written to the Openfire logs. Info level logging is sent to debug.
 */
public class JettyLog implements Logger {

    /**
     * Only enable Jetty debug logging if it's specifically enabled. Otherwise, Jetty debug logs
     * pollute the Openfire debug log with too much data.
     */
    private boolean debugEnabled = JiveGlobals.getBooleanProperty("jetty.debugEnabled");

    public boolean isDebugEnabled() {
        return debugEnabled && Log.isDebugEnabled();
    }

    public void setDebugEnabled(boolean b) {
        // Do nothing.
    }

    public void info(String string, Object object, Object object1) {
        // Send info log messages to debug because they are generally not useful.
        Log.debug(string);
    }

    public void debug(String string, Throwable throwable) {
        Log.debug(string, throwable);
    }

    public void debug(String string, Object object, Object object1) {
        Log.debug(string);
    }

    public void warn(String string, Object object, Object object1) {
        Log.warn(string);
    }

    public void warn(String string, Throwable throwable) {
        Log.warn(string, throwable);
    }

    public Logger getLogger(String string) {
        return new JettyLog();
    }
}
