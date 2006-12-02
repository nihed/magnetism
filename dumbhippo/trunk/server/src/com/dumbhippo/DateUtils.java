package com.dumbhippo;

import java.text.DateFormat;
import java.util.Date;

import org.apache.tomcat.util.http.FastHttpDateFormat;

public class DateUtils {
	
	public static long parseHttpDate(String date)  {
		return FastHttpDateFormat.parseDate(date, new DateFormat[] {});
	}
	
	public static String formatTimeAgo(Date timestamp) {
		// We might want to suppress more of the surrounding text at times
		// for unknown timestamps (0/-1) but displaying nothing is better
		// than displaying "36 years ago" or "at an unknown time"
		if (timestamp.getTime() <= 0)
			return "";
		
		Date now = new Date();

		long deltaSeconds = (now.getTime() - timestamp.getTime()) / 1000;
		
		if (deltaSeconds < 0)
			return "the future";
		
		if (deltaSeconds < 120)
			return "a minute ago";
			
		if (deltaSeconds < 60*60) {
			long deltaMinutes = deltaSeconds / 60;
			if (deltaMinutes < 5) {
				return Math.round(deltaMinutes) + " min. ago";
			} else {
				deltaMinutes = deltaMinutes - (deltaMinutes % 5);
				return Math.round(deltaMinutes) + " min. ago";
			}
		}

		long deltaHours = deltaSeconds / (60 * 60);
		
		if (deltaHours < 1.55) {
			return "1 hr. ago";
		} 

		if (deltaHours < 24) {
			return Math.round(deltaHours) + " hrs. ago";
		}

		if (deltaHours < 48) {
			return "Yesterday";
		}
		
		if (deltaHours < 24*15) {
			return Math.round(deltaHours / 24) + " days ago";
		}
		
		long deltaWeeks = deltaHours / (24*7);
		
		if (deltaWeeks < 6) {
			return Math.round(deltaWeeks) + " weeks ago";
		}
		
		if (deltaWeeks < 50) {
			return Math.round(deltaWeeks / 4) + " months ago";
		}
		
		long deltaYears = deltaWeeks / 52;
		
		if (deltaYears < 1.55) {
			return "1 year ago";
		} else {
			return Math.round(deltaYears) + " years ago";
		}		
	}
}