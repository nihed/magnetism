package com.dumbhippo;

/**
 * A utility class for formatting an age String.
 * 
 * @author marinaz
 */

public class AgeUtils {
	
	/*  
	 * Returns an age String in a human readable format.
	 *  
	 *  FIXME see also DateUtils.formatTimeAgo
	 *  
	 * @param age age in seconds
	 * @return age String in a human readable format
	 */
	public static String formatAge(long age) {
        // this output can be refined
		// this is an integer division, so the result will be rounded down,
		// which is what we want
        int days = (int)(age / (24 * 3600));
        if (days == 0) {
        	return "within 24 hours";
        } else if (days == 1) {
        	return "1 day ago";       	
        } else {
        	return days + " days ago";
        }    				
	}

}
