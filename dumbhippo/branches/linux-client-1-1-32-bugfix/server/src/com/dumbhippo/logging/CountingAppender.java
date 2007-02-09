package com.dumbhippo.logging;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.jboss.logging.appender.DailyRollingFileAppender;

public class CountingAppender extends DailyRollingFileAppender {
	static long debugCount = 0;
	static long infoCount = 0;
	static long warnCount = 0;
	static long errorCount = 0;
	
	static private synchronized void countEvent(LoggingEvent event) {
		switch (event.getLevel().toInt()) {
		case Level.DEBUG_INT:
			debugCount++;
			break;
		case Level.INFO_INT:
			infoCount++;
			break;
		case Level.WARN_INT:
			warnCount++;
			break;
		case Level.ERROR_INT:
			errorCount++;
			break;
		}
	}
	
	static public synchronized long getDebugCount() {
		return debugCount;
	}
	
	static public synchronized long getInfoCount() {
		return infoCount;
	}

	static public synchronized long getWarnCount() {
		return warnCount;
	}

	static public synchronized long getErrorCount() {
		return errorCount;
	}

	@Override
	public void append(LoggingEvent event) {
		countEvent(event);
		super.append(event);
	}
}
