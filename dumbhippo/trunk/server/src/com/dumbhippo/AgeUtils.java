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
	 * @param age age in seconds
	 * @return age String in a human readable format
	 */
	public static String formatAge(long age) {
        // this output can be refined
        int days = (int)Math.floor(age / (24 * 3600));
        if (days == 0) {
        	return "within 24 hours";
        } else if (days == 1) {
        	return "1 day ago";       	
        } else {
        	return days + " days ago";
        }    				
	}

}
